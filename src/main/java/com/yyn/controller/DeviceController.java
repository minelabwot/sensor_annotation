package com.yyn.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.yyn.dao.DatasetContainer;
import javafx.beans.binding.IntegerBinding;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yyn.model.Device;
import com.yyn.model.User;
import com.yyn.service.DeviceService;
import org.springframework.web.context.support.WebApplicationContextUtils;

@Controller
@RequestMapping("/device*.do")
public class DeviceController {
	@Autowired
	DeviceService ds;

	//添加tdb版本12-14更新
	@RequestMapping("/devicePropertyAdd.do")
	public String addProperty2mysql(HttpServletRequest request, Model model) {
		Dataset tdbdataset = ((DatasetContainer) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		
		User user = (User)request.getSession().getAttribute("userInfo");
		String owner = user.getName();
		Map<String,String> map = new HashMap<>();
		int id = -1;
		String deviceType = request.getParameter("deviceType");
		String name = request.getParameter("device[name]"); map.put("name",name);
		String description = request.getParameter("device[description]");
		String sensorType = request.getParameter("device[type]"); map.put("hasType",sensorType);
		String unit = request.getParameter("device[unit]"); map.put("hasUnit",unit);
		String property = request.getParameter("device[property]");map.put("forProperty",property);
		String region = request.getParameter("device[region]"); map.put("hasLocation",region);
		String spot = request.getParameter("device[spot]");map.put("hasSpot", spot);
		String company = request.getParameter("device[company]"); map.put("isOwnedBy",company);
		
		
		Properties info = new Properties();
		String elroot = request.getSession().getServletContext().getRealPath("/WEB-INF/ELApp/");
		
		try {
			info.load(new FileInputStream(elroot+"el_info.properties"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String table_id = info.getProperty("table_id");
		System.out.println("当前table_id为:"+table_id);

		//neo4j
		id = ds.addDataProperty(deviceType, owner, name, description);
		ds.addObjectProperty(map,id);
		//mysql
		ds.addDevice2mysql(new Device(id,deviceType,name,description,property, sensorType, unit, region,spot, company, owner,table_id));
		//tdb
		ds.add2TDB(id,request,tdbdataset, map, deviceType);
		
		return "servicePage/deviceEdit.jsp";
	}
	
	//show all device of one/all user
	@RequestMapping("/deviceShowAll.do")
	public String showAllDevice(HttpServletRequest request, Model model, String status) {
		String owner = null;
		if("all".equals(status)) {
			owner = "allUser";
			model.addAttribute("status","all");
		}
		else if ("current".equals(status)){
			User user = (User)request.getSession().getAttribute("userInfo");
			owner = user.getName();
			System.out.println("当前用户为:"+owner);
		}
		model.addAttribute("sensors", ds.showAllDevice(owner,"Sensor"));
		model.addAttribute("actuators", ds.showAllDevice(owner,"Actuator"));
		return "servicePage/deviceList.jsp";
	}
	
	
	@RequestMapping("/deviceDetail.do")
	public String showDetail(String id, String deviceType, Model model) {
		Map<String,String> map = new HashMap<>();
		map.put("id",id);
		ds.showDetail(id,map);
		model.addAttribute("avp", map);
		model.addAttribute("id", id);
		model.addAttribute("deviceType",deviceType);
		return "servicePage/detail.jsp";
	}

	@RequestMapping("/deviceActionAdd.do")
	public String addOperation(HttpServletRequest request,Model model) {
		Map<String,String[]> map = request.getParameterMap();
		String id = map.get("id")[0];
		String[] name = map.get("action_name[]");
		String[] url = map.get("action_urlTemplate[]");
		String[] param = map.get("action_messageContent[]");
		String[] lifecycle = map.get("action_lifecycle[]");
		String[] effect = map.get("action_effect[]");
		Dataset dataset = ((DatasetContainer) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		System.out.println(name.length);
		for(int i=0;i<name.length;++i) {
			System.out.println(id+"_"+name[i]+"_"+url[i]+"_"+param[i]+"_"+lifecycle[i]+"_"+effect[i]);
			ds.addAction2TDB(id, name[i], url[i], param[i], lifecycle[i], effect[i], dataset);
		}
		return showDetail(id,"actuator",model);
	}

	@RequestMapping("/deviceSearching.do")
	public String searchSomeThing(HttpServletRequest request,Model model) {
		Dataset dataset = ((DatasetContainer)WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).
				getBean("datasetContainer")).getDataset();
		String searchType = request.getParameter("searchType");
		String firkey = request.getParameter("first_key");
		String firval = request.getParameter("first_value");
		String seckey = request.getParameter("second_key");
		String secval = request.getParameter("second_value");
		
		ResultSet rs = null;
		
		if("Anomaly".equals(searchType)) {
			System.out.println("searching Anomaly");
			rs = ds.getResult(dataset, searchType, firkey, firval, seckey, secval);
			List<List<String>> list = new ArrayList<>();
			while(rs.hasNext()) {
				QuerySolution qs = rs.next();
				List<String> tmp = new ArrayList<>();
				tmp.add(qs.get("deviceID").toString());
				tmp.add(qs.get("device").asResource().getLocalName());
				tmp.add(qs.get("anomaly").asResource().getLocalName());
				tmp.add(qs.get("cause").asResource().getLocalName());
				tmp.add(qs.get("time").toString().split("\\.")[0]);
				list.add(tmp);
			}
			model.addAttribute("anomalys", list);
		}
		else if("Device".equals(searchType)) {
			System.out.println("searching Device");
			rs = ds.getResult(dataset, searchType, firkey, firval, seckey, secval);
			List<String> list = new ArrayList<>();
			while(rs.hasNext()) {
				list.add(rs.next().get("deviceID").toString());
			}
			List<Device> devices = new ArrayList<>();
			for(String id : list) {
				devices.add(ds.getDeviceInfo(id));
			}
			model.addAttribute("devices", devices);
		}
		return "forward:/index.jsp";
	}
}
