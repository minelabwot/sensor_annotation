package com.yyn.controller;

import javax.servlet.http.HttpServletRequest;

import com.yyn.dao.DatasetContainer;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yyn.service.AutoControlService;
import com.yyn.util.RDFReasoning;
import org.springframework.web.context.support.WebApplicationContextUtils;

@Controller
@RequestMapping("/auto*.do")
public class AutoControlController {
	@Autowired
	private AutoControlService acs;
	
	@RequestMapping("/autoGenerator.do")
	public String generateAction(HttpServletRequest request) {
		Dataset ds = ((DatasetContainer) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		acs.generateControlModel(ds,request);
		RDFReasoning.output(ds);
		return "redirect:/index.jsp";
	}
}
