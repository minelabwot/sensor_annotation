package com.yyn.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yyn.config.NameSpaceConstants;
import com.yyn.dao.DatasetContainer;
import com.yyn.service.AnomalyService;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yyn.service.AutoControlService;
import com.yyn.util.RDFReasoning;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Controller
@RequestMapping("/")
public class AutoControlController {
	@Autowired
	private AutoControlService acs;

	@Autowired
	private AnomalyService as;

	@RequestMapping(value = "autoGenerator.do",produces = "application/json;charset=UTF-8")
	public String generateAction(HttpServletRequest request) {
		Dataset ds = ((DatasetContainer) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		acs.generateControlModel(ds,request);
		RDFReasoning.output(ds);
		return "redirect:/index.jsp";
	}

	@RequestMapping(value = "device/{id}/set/{value}", method = RequestMethod.GET)
	public void callAction(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String actuator_id, @PathVariable("value") String value) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		float val = Float.parseFloat(value);
		System.out.println("开始降温");
		//在这里添加异常判断
		String anomalyConfigPath = request.getSession().getServletContext().getRealPath("/WEB-INF/config/anomaly_config.properties");
		Properties proper = new Properties();
		try {
			proper.load(new FileInputStream(anomalyConfigPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		float temp_high = Float.parseFloat(proper.get("temp_high").toString());
		float temp_low = Float.parseFloat(proper.get("temp_low").toString());
		Dataset dataset = ((DatasetContainer)WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();

		dataset.begin(ReadWrite.READ);
		String queryString = StrUtils.strjoinNL(
				NameSpaceConstants.PREFIX,
				"SELECT DISTINCT ?id ?sensor ?pro",
				"WHERE { GRAPH wot:sensor_annotation { ?a wot:deviceID '"+actuator_id+ "'^^xsd:string. ",
				"?a wot:subscribe ?anomaly. ",
				"?sensor ssn:detects ?anomaly. ",
				"?type a wot:EntityType. ",
				"?sensor wot:hasType ?type. ",
				"?type wot:defaultObserved ?pro. ",
				"?sensor wot:deviceID ?id",
				"} }");
		QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset);
		ResultSet rs = qExec.execSelect();
		List<String> name = new ArrayList<>();
		List<String> pro = new ArrayList<>();
		List<String> id = new ArrayList<>();

		while(rs.hasNext()) {
			QuerySolution qs = rs.next();
			name.add(qs.get("a").asResource().getLocalName());
			pro.add(qs.get("pro").asResource().getLocalName());
			id.add(qs.get("id").asLiteral().getString());
			System.out.println(name.get(name.size()-1)+"_"+id.get(name.size()-1));
		}
		qExec.close();
		dataset.commit();
		dataset.end();

		//写入当前设备的当前的温度值
		dataset.begin(ReadWrite.WRITE);

		try {
			//每次更新传感值时同步
			for(int i=0;i<id.size();++i) {
				String update = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
						"DELETE { GRAPH wot:sensor_annotation { ?device wot:hasValue ?val} } ",
						"INSERT { ",
						"GRAPH wot:sensor_annotation {",
						"?device wot:hasValue '" + val + "'^^xsd:float} } ",
						"USING wot:sensor_annotation ",
						"WHERE {?device wot:deviceID '" + id.get(i) + "'^^xsd:string. ",
						"?device ?rel ?val"
						,
						"}"); //用于删除之前的选择,并且兼顾如果初始没有hasValue的情况
				RDFReasoning.updateQuery(update, dataset);
			}
			dataset.commit();
		} finally {
			dataset.end();
		}

		//产生设备当前状态
		as.createState(dataset);

		//只有温度,等检测性传感器可以产生异常
		if("Temperature".equals(pro)) {
			if(val > temp_high || val < temp_low) {
				dataset.begin(ReadWrite.WRITE);
				String s = null;
				if(val < temp_low) {
					s = "low";
					System.out.println("产生了低温异常");
				}
				else if(val > temp_high) {
					s = "high";
					System.out.println("产生了高温异常");
				}
				String update = StrUtils.strjoinNL(
						NameSpaceConstants.PREFIX,
						"DELETE { GRAPH wot:sensor_annotation { ?device ssn:detects ?event. ",
						"?anomaly ssn:observationSamplingTime ?time. } }",
						"INSERT { GRAPH wot:sensor_annotation { ?anomaly rdf:type wot:Anomaly. ",
						//同时生成致因实体
						"?cause rdf:type wot:ObservedCause. ",
						"?cause ssn:observationSamplingTime '"+time.toString()+"'^^xsd:dataTime. ",
						"?device ssn:detects ?anomaly. ",
						"?anomaly ssn:observationSamplingTime '"+time.toString()+"'^^xsd:dataTime. } }",
						"USING wot:sensor_annotation ",
						"WHERE { ?device wot:deviceID '"+id+"'^^xsd:string .",
						"?event a ?eventCls. ",
						"?sub ?rel ?time. ",
						"BIND(URI('"+ NameSpaceConstants.WOT+"temp_"+s+"_"+id+"_cause') as ?cause). ",
						"BIND(URI('"+NameSpaceConstants.WOT+"temp_"+s+"_"+id+"') as ?anomaly).",
						"}");
				RDFReasoning.updateQuery(update, dataset);
				dataset.commit();
				dataset.end();
			}
			else {
				dataset.begin(ReadWrite.WRITE);
				String update = StrUtils.strjoinNL(
						NameSpaceConstants.PREFIX,
						"DELETE { GRAPH wot:sensor_annotation { ?device wot:detects ?event. } } ",
						"USING wot:sensor_annotation ",
						"WHERE { ?device wot:deviceID '"+id+"'^^xsd:string .",
						"?event a ?eventCls. ",
						"?eventCls rdfs:subClassOf ssn:Stimulus. }");
				RDFReasoning.updateQuery(update, dataset);
				dataset.commit();
				dataset.end();
			}
		}
		response.setStatus(201);
	}
}
