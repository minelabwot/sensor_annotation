package com.yyn.service;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.yyn.config.NameSpaceConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import com.yyn.util.RDFReasoning;

@Service
public class AutoControlService {
	
	public void generateControlModel(Dataset ds, HttpServletRequest request) {
		createSubscribe(ds,request);
		changeLifecycle(ds);
		//generateAction(ds);
		callAction(ds);
	}
	
	private void createSubscribe(Dataset ds,HttpServletRequest request) {
		ds.begin(ReadWrite.WRITE);
		try {
			String updateString = StrUtils.strjoinNL( NameSpaceConstants.PREFIX,
					"DELETE { GRAPH wot:sensor_annotation { ?actuator wot:subscribe ?event.} }",
					"INSERT { GRAPH wot:sensor_annotation { ?actuator wot:subscribe ?anomaly. } }",
					"USING  wot:sensor_annotation ",
					"WHERE { ?actuator a san:Actuator. ",
					"?feature wot:hasDevice ?actuator. ",
					"?sensor a ssn:Sensor. ",
					"?feature wot:hasDevice ?sensor. ",
					"?sensor ssn:detects ?anomaly. ",
					"?anomaly a wot:Anomaly. ",
					"?actuator ?rel ?event. }"
					);
			RDFReasoning.updateQuery(updateString, ds);
			ds.commit();
		} finally {
			ds.end();
		}
	}
	private void changeLifecycle(Dataset ds) {
		ds.begin(ReadWrite.WRITE);
		try {

			String query1 = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					"DELETE { GRAPH wot:sensor_annotation { ?actuator wot:currentState ?sub. } }",
					"INSERT { GRAPH wot:sensor_annotation { ?actuator wot:currentState ?actuator_state. } }",
					"USING  wot:sensor_annotation ",
					"WHERE { ?actuator wot:subscribe ?anomaly. ",
					"?anomaly a wot:Anomaly. ",
					"?sensor ssn:detects ?anomaly. ",
					"?sensor wot:currentStatus ?status. ",
					"?status wot:hasValue ?status_value. ",
					"?actuator wot:hasState ?actuator_state. ",
					"?actuator_state wot:hasValue ?state_value2. ",
					"?actuator msm:hasOperation ?actuation. ",
					"?actuation a san:Actuation. ",
					"?actuator ?rel ?sub. ",//用于删除先前状态
					"FILTER (?status_value * ?state_value2 = -1). ",
					"}");
			String query2 = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					"DELETE { GRAPH wot:sensor_annotation { ?actuator wot:currentState ?sub. } }",
					"INSERT { GRAPH wot:sensor_annotation { ?actuator wot:currentState wot:off. } }",
					"USING  wot:sensor_annotation ",
					"WHERE { ?actuator wot:subscribe ?anomaly. ",
					"?anomaly a wot:Anomaly. ",
					"?sensor ssn:detects ?anomaly. ",
					"?sensor wot:currentStatus ?status. ",
					"?status wot:hasValue ?status_value. ",
					"?actuator wot:currentState ?actuator_state. ",
					"?actuator_state wot:hasValue ?state_value2. ",
					"?actuator msm:hasOperation ?actuation. ",
					"?actuation a san:Actuation. ",
					"?actuator ?rel ?sub. ",//用于删除先前状态
					"FILTER (?status_value = 0 && ?state_value2 != 0). ",
					"}");
			RDFReasoning.updateQuery(query1,ds);
			RDFReasoning.updateQuery(query2,ds);
		} finally {
			ds.commit();
		}
		ds.end();
	}

	private void callAction(Dataset ds) {
		ds.begin(ReadWrite.READ);
		String query = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
				"SELECT DISTINCT ?url ?param ",
				"WHERE { GRAPH wot:sensor_annotation { ?actuator wot:subscribe ?anomaly. ",
				"?anomaly a wot:Anomaly. ",
				"?sensor ssn:detects ?anomaly. ",
				"?sensor wot:currentStatus ?status. ",
				"?status wot:hasValue ?status_value. ",
				"?actuator msm:hasOperation ?actuation. ",
				"?actuation dul:includesEvent ?postCondition. ",
				"?postCondition wot:resultIn ?effect_value. ",
				"?actuation wot:hasAddress ?url. ",
				"?actuation wot:hasParam ?param. ",
				"FILTER (?status_value * ?effect_value = -1). ",
				"} }");
		QueryExecution qExec = QueryExecutionFactory.create(query, ds);
		ResultSet rs = qExec.execSelect();
		String url = null;
		String param = null;
		if (rs.hasNext()) {
			QuerySolution qs = rs.next();
			url = qs.getLiteral("url").getString();
			param = qs.getLiteral("param").getString();
			System.out.println("调用的服务是->"+url+"_"+param);
		}
		while(rs.hasNext()) {
			System.out.println("error,2 action contains in config");
			QuerySolution qs = rs.next();
			String url_e = qs.getLiteral("url").getString();
			String param_e = qs.getLiteral("param").getString();
			System.out.println("未被处理的调用服务是->"+url_e+"_"+param_e);
		}
		qExec.close();
		callRestfulService(url,param);

	}
	/*
	private void generateAction(Dataset ds) {
		ds.begin(ReadWrite.WRITE);
		
		String delete = "DELETE{ ?uri ssn:observationSamplingTime ?val. "
				+ "?actuator wot:triggers ?event. }";
		String part1 = StrUtils.strjoinNL("?uri ssn:observationSamplingTime ?time. ",
				"?actuator wot:triggers ?uri. }",
				"WHERE { ?actuator ssn:forProperty ?prop1. ",
				"?actuator wot:subscribe ?anomaly. ",
				"?anomaly ssn:observationSamplingTime ?time. ",
				"?sensor wot:generate ?anomaly. ",
				"?sensor ssn:forProperty ?prop2. ");
		String part2 = StrUtils.strjoinNL("?proc ssn:hasInput ?prop1. ",
				"?proc ssn:hasOutput ?prop2. ");
		String part3 = StrUtils.strjoinNL("?sub ssn:observationSamplingTime ?val. ",
				"?actuator ?rel ?event. ");
		try {
			String negHigh = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					delete,
					"INSERT { ?uri rdf:type wot:TurnUp. ",//dute
					part1,
					"?proc a wot:NegativeCorrelationProcess. ",//dute
					part2,
					"?sensor wot:hasState wot:high. ",//dute
					part3,
					"BIND(URI(CONCAT(str(?actuator),'_action_up')) as ?uri ) }"
					);
			String negLow = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					delete,
					"INSERT { ?uri rdf:type wot:TurnDown. ",//dute
					part1,
					"?proc a wot:NegativeCorrelationProcess. ",//dute
					part2,
					"?sensor wot:hasState wot:low. ",//dute
					part3,
					"BIND(URI(CONCAT(str(?actuator),'_action_down')) as ?uri ) }"
					);
			String posHigh = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					delete,
					"INSERT { ?uri rdf:type wot:TurnDown. ",//dute
					part1,
					"?proc a wot:PositiveCorrelationProcess. ",//dute
					part2,
					"?sensor wot:hasState wot:high. ",//dute
					part3,
					"BIND(URI(CONCAT(str(?actuator),'_action_down')) as ?uri ) }"
					);
			String posLow = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					delete,
					"INSERT { ?uri rdf:type wot:TurnUp. ",//dute
					part1,
					"?proc a wot:PositiveCorrelationProcess. ",//dute
					part2,
					"?sensor wot:hasState wot:low. ",//dute
					part3,
					"BIND(URI(CONCAT(str(?actuator),'_action_up')) as ?uri ) }"
					);
			RDFReasoning.updateQuery(negHigh, ds);
			RDFReasoning.updateQuery(negLow, ds);
			RDFReasoning.updateQuery(posHigh, ds);
			RDFReasoning.updateQuery(posLow, ds);
			ds.commit();
		} finally {
			ds.end();
		}
	}
	*/

	private void callRestfulService(String url,String param) {
		if(StringUtils.isEmpty(url) || StringUtils.isEmpty(param))
			return;
		String[] params = param.split(",");
		String requestUrl = url;
		int index = 0;
		while(requestUrl.contains("$$"))
			requestUrl = requestUrl.replaceFirst("\\$\\$", params[index++]);
		String response = sendGet("http://"+requestUrl);
		System.out.println(response);
	}

	/**
	 * 向指定URL发送GET方法的请求
	 *
	 * @param url
	 *            发送请求的URL
	 * @return URL 所代表远程资源的响应结果
	 */
	private String sendGet(String url) {
		String result = "";
		BufferedReader in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection connection = realUrl.openConnection();
			// 设置通用的请求属性
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			// 建立实际的连接
			connection.connect();
			// 获取所有响应头字段
			Map<String, List<String>> map = connection.getHeaderFields();
			// 遍历所有的响应头字段
			for (String key : map.keySet()) {
				System.out.println(key + "--->" + map.get(key));
			}
			// 定义 BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			System.out.println("发送GET请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输入流
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}
}
