package com.yyn.service;

import com.yyn.config.NameSpaceConstants;
import com.yyn.dao.DataCollectionDAO;
import com.yyn.model.Device;
import com.yyn.model.Relation;
import com.yyn.model.Rule;
import com.yyn.model.Struct;
import com.yyn.util.RDFReasoning;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.neo4j.driver.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DeviceService {
	
	private List<String> unDisplayProperty = new ArrayList<String>();
	
	@Autowired
	private DataCollectionDAO dc;
	
	public DeviceService() {
		unDisplayProperty.add("type");
		unDisplayProperty.add("ref");
	}
	//向关系型数据库添加设备信息
	public void addDevice2mysql(Device device) {
		dc.insertNewDevice(device);
	}
	
	//添加数据属性
	public int addDataProperty(String deviceType, String owner, String name, String desc) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		String statement = "match (a:Class {label:\""+deviceType+"\"}) merge (p:Device {name:\""+name+"\",description:\""+desc
		+"\",user:\""+owner+"\",ref:\""+NameSpaceConstants.WOT+name+"\"}) merge (p)-[:NamedIndividual {ref:\"http://www.w3.org/2002/07/owl#NamedIndividual\",label:\"NamedIndividual\"}]->(a) return id(p)";
		try {
			StatementResult sr =  session.run(statement);
			if(sr.hasNext())
				
			return sr.next().get("id(p)").asInt();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				session.close();
				driver.close();
			}
		return -1;
	}
	
	//ver 2016 11,11
	public void addObjectProperty(Map<String,String> map,int id) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		String temp = "";
		String temp2 = "";
		int index = 0;
		for(String s : map.keySet()) {
			System.out.println(s);
			switch(s) {
				case "hasType":
					temp2 += ",(c"+index+":Class {label:\"EntityType\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
				case "hasUnit": 
					temp2 += ",(c"+index+":Class {label:\"Unit\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
				case "hasLocation": 
					temp2 += ",(c"+index+":Class {label:\"Region\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
				case "hasSpot": 
					temp2 += ",(c"+index+":Class {label:\"Spot\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
				case "isOwnedBy": 
					temp2 += ",(c"+index+":Class {label:\"Owner\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
				case "forProperty": 
					temp2 += ",(c"+index+":Class {label:\"SensorProperty\"})";
					temp += " merge (p"+index+":Instance {label:\""+map.get(s)+"\"}) "
						+ "merge (d)-[:"+s+"]->(p"+index+") merge (p"+index+")-[:NamedIndividual]->(c"+index+")";
					++index;
					break;
			}
		}
		String statement = "match (d)"+temp2+"where id(d)="+id+temp;
		try {
			session.run(statement);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				session.close();
				driver.close();
			}
	}
	
	public List<Device> showAllDevice(String owner,String type) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		List<Device> list = new ArrayList<Device>();
		
		try {
			String statement;
			if(owner.equals("allUser"))
				statement = "match (d:Device)-[:NamedIndividual]->(:Class {label:\""+type+"\"})  return d,id(d)";
			else
				statement = "match (d:Device {user:\""+owner+"\"})-[:NamedIndividual]->(:Class {label:\""+type+"\"})  return d,id(d)";
			StatementResult result = session.run(statement);
			while (result.hasNext()) {
				Record r = result.next();
				int id = r.get("id(d)").asInt();
				Map<String,Object> map = r.get("d").asMap();
				Device d = new Device(id, map.get("name").toString(), map.get("description").toString());
				list.add(d);
			}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				session.close();
				driver.close();
			}
		System.out.println(list.size());
		return list;
	}
	
	public Device getDeviceInfo(String device_id) {
		return dc.getDeviceInfo(device_id);
	}

	public void showDetail(String id,Map<String,String> resultmap) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		try {
			String statement = "match (d) where id(d)="+id+" return d";
			StatementResult result = session.run(statement);
			while (result.hasNext()) {
				Record r = result.next();
				Map<String,Object> map = r.get("d").asMap();
				for(String s :map.keySet()) {
					if(!unDisplayProperty.contains(s)) 
						resultmap.put(s, map.get(s).toString());
				}
			}
			
			statement = "match (d)-[r]->(o) where id(d)="+id+" return type(r),o";
			result = session.run(statement);
			while(result.hasNext()) {
				Record r = result.next();
				String rName = r.get("type(r)").asString();
				Map<String,Object> proper = r.get("o").asMap();
				if(!proper.get("label").equals("rule"))
					resultmap.put(rName, proper.get("label").toString());
			}

			statement = "match (o)-[r]->(d) where id(d)="+id+" return type(r),o";
			result = session.run(statement);
			while(result.hasNext()) {
				Record r = result.next();
				String rName = r.get("type(r)").asString();
				Map<String,Object> proper = r.get("o").asMap();
				resultmap.put(rName, proper.get("label").toString());
			}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				session.close();
				driver.close();
			}
	}
	
	public Device getLink(String id) {
		return dc.getLink(id);
	}

	public void addLink2Neo_instance(Device device) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		String location = device.getRegion().split(",")[0];
		String statement = "start d=node("+device.getId()+")"
				+ " match (d)-[:hasLocation]->(l), (d)-[:hasUnit]->(u),(d)-[:isOwnedBy]->(c)"
				+ ",(d)-[:hasType]->(t),(d)-[:forProperty]->(o) "
				+ "merge (l1:DBpedia {link:\""+location+"\"}) "
				+ "merge (u1:DBpedia {link:\""+device.getUnit().split(",")[0]+"\"}) "
				+ "merge (c1:DBpedia {link:\""+device.getCompany().split(",")[0]+"\"}) "
				+ "merge (t1:DBpedia {link:\""+device.getSensorType().split(",")[0]+"\"}) "
				+ "merge (o1:DBpedia {link:\""+device.getProperty().split(",")[0]+"\"}) "
				+ "merge (l)-[:linkTo]->(l1) "
				+ "merge (u)-[:linkTo]->(u1) "
				+ "merge (c)-[:linkTo]->(c1) "
				+ "merge (t)-[:linkTo]->(t1) "
				+ "merge (o)-[:linkTo]->(o1)";
		try {
			session.run(statement);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
			driver.close();
		}
	}
	
	public Device getColumnLink(String id) {
		return dc.getColumn(id);
	}
	public void addLink2Neo_column(Device device) {
		Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "930208" ) );
		Session session = driver.session();
		String location = device.getRegion();
		String observation = device.getProperty();
		String type = device.getSensorType();
		String company = device.getCompany();
		String unit = device.getUnit();
		String statement = "match (l:Class {label:\"Region\"}), (u:Class {label:\"Unit\"}),(c:Class {label:\"Owner\"})"
				+ ",(t:Class {label:\"EntityType\"}),(o:Class {label:\"SensorProperty\"}) ";
		if(!location.equals("init"))
				statement += "merge (l1:DBpedia {link:\""+location+"\"}) "
						+ "merge (l)-[:equalsTo]->(l1) ";
		if(!unit.equals("init"))
				statement += "merge (u1:DBpedia {link:\""+unit+"\"}) "
						+ "merge (u)-[:equalsTo]->(u1) ";
		if(!company.equals("init"))
				statement += "merge (c1:DBpedia {link:\""+company+"\"}) "
						+ "merge (c)-[:equalsTo]->(c1) ";
		if(!type.equals("init"))
				statement += "merge (t1:DBpedia {link:\""+type+"\"}) "
						+ "merge (t)-[:equalsTo]->(t1) ";
		if(!observation.equals("init"))
				statement +=  "merge (o1:DBpedia {link:\""+observation+"\"}) "
						+ "merge (o)-[:equalsTo]->(o1)";
		statement += " return l";
		try {
			session.run(statement);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
			driver.close();
		}
	}
	
	//TDB method
	public void add2TDB(int id,HttpServletRequest request,Dataset ds,Map<String,String> metadata_avp,String deviceType) {
		ds.begin(ReadWrite.WRITE);
		String device_type;
		String state;
		if("Sensor".equals(deviceType)) {
			device_type = "ssn:" + deviceType;
			state = "?device wot:currentStatus wot:nomal. ";
		}
		else {
			device_type = "san:" + deviceType;
			state = "?device wot:currentState wot:off. " +
			"?device wot:currentStatus wot:nomal. ";
		}

		try {
			String update = StrUtils.strjoinNL(
					NameSpaceConstants.PREFIX,
					"INSERT { ",
						"GRAPH wot:sensor_annotation {",
						"?device rdf:type "+device_type+". ",
						"?device wot:deviceID \""+id+"\"^^xsd:string.",
						state,
						//region
						"?region rdf:type wot:Region. ",
						"?device dul:hasLocation ?region. ",
						//spot
						"?spot rdf:type wot:Spot. ",
						"?device wot:hasSpot ?spot. ",
						//unit
						"?unit rdf:type wot:Unit. ",
						"?device wot:hasUnit ?unit. ",
						//entityType
						"?type rdf:type wot:EntityType. ",
						"?device wot:hasType ?type. ",
						"?type wot:defaultObserved wot:"+metadata_avp.get("forProperty")+". ",
						//owner
						"?owner rdf:type wot:Owner. ",
						"?device wot:isOwnedBy ?owner. }",
					" } USING wot:sensor_annotation ",
					" WHERE {",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("name")+"\")) as ?device). ",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("hasLocation")+"\")) as ?region). ",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("hasSpot")+"\")) as ?spot). ",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("hasUnit")+"\")) as ?unit). ",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("isOwnedBy")+"\")) as ?owner). ",
					"BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",\""+metadata_avp.get("hasType")+"\")) as ?type) }");
			RDFReasoning.updateQuery(update, ds);
			ds.commit();
		} finally { 
			ds.end();
		}
		RDFReasoning.output(ds);
	}
	//add action
	public void addAction2TDB(String id , String name,String url,String param,String lifecycle,String effect,Dataset ds) {
		Map<String,String> map = new HashMap<>();
		map.put("increment","1");
		map.put("decrement","-1");

	    ds.begin(ReadWrite.WRITE);
		try {
			String update = StrUtils.strjoinNL(
                    NameSpaceConstants.PREFIX,
                    "INSERT { ",
                    "GRAPH wot:sensor_annotation {",
                    "?device msm:hasOperation ?operation. ",
                    "?operation a san:Actuation. ",
                    "?operation wot:hasAddress \""+url+"\"^^xsd:string. ",
                    "?operation wot:hasParam \""+param+"\"^^xsd:string. ",
                    "?lifecycle a wot:State. ",
                    "?lifecycle wot:hasValue \""+map.get(effect)+"\"^^xsd:int. ",
                    "?device wot:hasState ?lifecycle. ",
                    "?effect a wot:PostCondition. ",
                    "?device wot:triggers ?effect. ",
                    "?effect wot:resultIn \""+map.get(effect)+"\"^^xsd:int. ",
					"?operation dul:includesEvent ?effect. }",
                    " } USING wot:sensor_annotation ",
                    " WHERE {",
                    "?device wot:deviceID '"+id+ "'^^xsd:string. ",
                    "?device wot:hasType ?entityType. ",
                    "BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",STRAFTER(str(?device),'#'),'_',\""+name+"\")) as ?operation). ",
                    "BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",STRAFTER(str(?entityType),'#'),'_',\""+lifecycle+"\")) as ?lifecycle). ",
                    "BIND(URI(CONCAT(\""+NameSpaceConstants.WOT+"\",STRAFTER(str(?device),'#'),'_',\""+effect+"\")) as ?effect). }");
			RDFReasoning.updateQuery(update, ds);

			ds.commit();
		} finally {
			ds.end();
		}
	}
	public void addELResult(String id,Dataset ds,Device device) {
		ds.begin(ReadWrite.WRITE);
		try {
			String update = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
					"INSERT {",
						"GRAPH wot:sensor_annotation { ",
							"?region wot:linkTo \""+ device.getRegion().split(",")[0] +"\"^^xsd:string. ",
							"?owner wot:linkTo \""+ device.getCompany().split(",")[0] +"\"^^xsd:string. ",
							"?unit wot:linkTo \""+ device.getUnit().split(",")[0] +"\"^^xsd:string. ",
							"?propCls wot:linkTo \""+ device.getProperty().split(",")[0] +"\"^^xsd:string. ",
							"?type wot:linkTo \""+ device.getSensorType().split(",")[0] +"\"^^xsd:string. ",
						"} ",
					"} USING wot:sensor_annotation ",
					" WHERE { ?device wot:deviceID \""+id+"\"^^xsd:string. ",
						"?device dul:hasLocation ?region. ",
						"?deivce wot:isOwnedBy ?owner. ",
						"?device wot:hasUnit ?unit. ",
						"?device wot:hasType ?type. ",
						"?type wot:defaultObserved ?propCls. ",
					"}");
			RDFReasoning.updateQuery(update, ds);
			ds.commit();
		} finally {
			ds.end();
		}
		RDFReasoning.output(ds);
	}
	
	public ResultSet getResult(Dataset ds,String searchType,String firkey,String firval,String seckey,String secval) {
		ds.begin(ReadWrite.READ);
		String location_local ="";
		String location_db = "";
		if("Region".equals(firkey)) {
			location_local += StrUtils.strjoinNL(
					"?device dul:hasLocation ?loc_local. ",
					"?loc_local wot:linkTo ?loc_el. ",
					"BIND(URI(?loc_el) as ?loc_el_uri). ",
					"?loc_local a wot:Region. ");
			location_db += StrUtils.strjoinNL("?loc_el_uri ?rel ?loc_db. ",
					"FILTER regex(str(?loc_db),\""+firval+"\") ");
		}
		else if("Region".equals(seckey)) {
			location_local += StrUtils.strjoinNL(
					"?device dul:hasLocation ?loc_local. ",
					"?loc_local wot:linkTo ?loc_el. ",
					"BIND(URI(?loc_el) as ?loc_el_uri). ",
					"?loc_local a wot:Region. ");
			location_db += StrUtils.strjoinNL("?loc_el ?rel ?loc_db. ",
					"FILTER regex(str(?loc_db),\""+secval+"\"). ");
		}
		
		String owner_local = "";
		String owner_db = "";
		if("Owner".equals(firkey)) {
			owner_local += StrUtils.strjoinNL(
					"?device wot:isOwnedBy ?owner_local. ",
					"?owner_local wot:linkTo ?owner_el. ",
					"BIND(URI(?owner_el) as ?owner_el_uri). ",
					"?owner_local a wot:Owner. ");
			owner_db += StrUtils.strjoinNL("?owner_el_uri ?rel ?owner_db. ",
					"FILTER regex(str(?owner_db),\""+firval+"\") ");
		}
		else if("Owner".equals(seckey)) {
			owner_local += StrUtils.strjoinNL(
					"?device wot:isOwnedBy ?owner_local. ",
					"?owner_local wot:linkTo ?owner_el. ",
					"BIND(URI(?owner_el) as ?owner_el_uri). ",
					"?owner_local a wot:Owner. ");
			owner_db += StrUtils.strjoinNL("?owner_el_uri ?rel ?owner_db. ",
					"FILTER regex(str(?owner_db),\""+secval+"\") ");
		}
		try {
			String query = null;
			if("Device".equals(searchType)) {
				query = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
						"SELECT DISTINCT ?deviceID ",
						"WHERE { ",
							"GRAPH wot:sensor_annotation {",
								"?device wot:deviceID ?deviceID. ",
								owner_local,
								location_local,
								"SERVICE <http://dbpedia.org/sparql> { ",
									owner_db,
									location_db,
								"}",
							"}",
						"}");
			}
			else if("Anomaly".equals(searchType)) {
				query = StrUtils.strjoinNL(NameSpaceConstants.PREFIX,
						"SELECT DISTINCT ?deviceID ?anomaly ?cause ?time ?device ",
						"WHERE { ",
							"GRAPH wot:sensor_annotation { ",
								"?device wot:deviceID ?deviceID. ",
								"?device wot:generate ?anomaly. ",
								"?anomaly wot:hasPotCause ?cause. ",
								"?anomaly ssn:observationSamplingTime ?time. ",
								"?cause ssn:observationSamplingTime ?time. ",
								owner_local,
								location_local,
								"SERVICE <http://dbpedia.org/sparql> { ",
									owner_db,
									location_db,
								"}",
							"}",
						"}");
			}
			System.out.println(query);
			ResultSet rs = RDFReasoning.selectQuery(query,ds);
			//ResultSetFormatter.out(rs);
			ds.commit();
			return rs;
		} finally {
			ds.end();
		}
	}
	
}







class StrutsComparator implements Comparator<Struct> {
	@Override
	public int compare(Struct o1, Struct o2) {
		// TODO Auto-generated method stub
		return o1.getrName().compareTo(o2.getrName());
		
	}
}
class RelationComparator implements Comparator<Relation> {
	@Override
	public int compare(Relation o1, Relation o2) {
		// TODO Auto-generated method stub
		return o1.getName().compareTo(o2.getName());
		
	}
}