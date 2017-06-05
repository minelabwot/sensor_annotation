package com.yyn.controller;

import com.yyn.config.NameSpaceConstants;
import com.yyn.dao.DatasetContainer;
import com.yyn.util.RDFReasoning;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AnomalyController {

	@RequestMapping("/Anomaly_showAllAnomaly.do")
	public String generateDiagnosisModel(HttpServletRequest request,Model model) {
		Dataset ds = ((DatasetContainer) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		Map<String,List<String>> causes = new HashMap<>();
		Map<String,String> times = new HashMap<>();
		ds.begin(ReadWrite.READ);
		try {
			String query = StrUtils.strjoinNL(
					NameSpaceConstants.PREFIX,
					"SELECT ?anomaly ?cause ?time",
					"WHERE { GRAPH wot:sensor_annotation { ?anomaly a wot:Anomaly.",
					"?anomaly wot:hasPotCause ?cause.",
					"?anomaly ssn:observationSamplingTime ?time. ",
					"?cause ssn:observationSamplingTime ?time. } }");
			ResultSet rs = RDFReasoning.selectQuery(query, ds);
			while(rs.hasNext()) {
				QuerySolution qs = rs.next();
				Resource ano = qs.get("anomaly").asResource();
				Resource cau = qs.get("cause").asResource();
				String time = qs.getLiteral("time").getString();
				System.out.println(ano.getURI());
				if(causes.containsKey(ano.getLocalName())){
					causes.get(ano.getLocalName()).add(cau.getLocalName());
				}
				else {
					List<String> list = new ArrayList<>();
					list.add(cau.getLocalName());
					causes.put(ano.getLocalName(), list);
				}
				times.put(ano.getLocalName(), time);
			}
			
			model.addAttribute("anomalys", causes);
			model.addAttribute("times", times);
			ds.commit();
		} finally { 
			ds.end();
		}
		return "servicePage/anomalyList.jsp";
	}
}
