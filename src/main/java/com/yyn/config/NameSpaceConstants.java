package com.yyn.config;

import org.apache.jena.atlas.lib.StrUtils;

public class NameSpaceConstants {
	public static final String WOT = "https://raw.githubusercontent.com/minelabwot/SWoT/master/swot-o.owl#";
	public static final String SSN = "http://purl.oclc.org/NET/ssnx/ssn#";
	public static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String DUL = "http://www.loa-cnr.it/ontologies/DUL.owl#";
	public static final String XSD = "http://www.w3.org/2001/XMLSchema#";
	public static final String SAN = "http://www.irit.fr/recherches/MELODI/ontologies/SAN#";
	public static final String MSM = "http://iserve.kmi.open.ac.uk/ns/msm#";
	public static final String PREFIX = StrUtils.strjoinNL(
			"PREFIX wot: <"+WOT+"> ",
			"PREFIX ssn: <"+SSN+"> ",
			"PREFIX rdf: <"+RDF+"> ",
			"PREFIX rdfs: <"+RDFS+"> ",
			"PREFIX dul: <"+DUL+"> ",
			"PREFIX xsd: <"+XSD+"> ",
			"PREFIX san: <"+SAN+"> ",
			"PREFIX msm: <"+MSM+"> ");
}
