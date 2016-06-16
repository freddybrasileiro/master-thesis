package br.ufes.inf.nemo.mlt.web.reasoner.sparql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import br.ufes.inf.nemo.mlt.web.vocabulary.MLT;

public class SparqlUtil {
	static public String owl = "owl";
	static public String rdfs = "rdfs";
	static public String rdf = "rdf";
	static public String xml = "xsd";
	
	static public String PREFIXES = ""
			+ "PREFIX " + owl + ": <" + OWL.getURI() + ">\n"
			+ "PREFIX " + rdfs + ": <" + RDFS.getURI() + ">\n"
			+ "PREFIX " + rdf + ": <" + RDF.getURI() + ">\n"
			+ "PREFIX " + xml + ": <" + XSD.getURI() + ">\n"
			+ "PREFIX " + MLT.getPrefix() + ": <" + MLT.getURI() + ">\n";
	
	public static boolean ask(OntModel model, Resource resource, Property property, RDFNode rdfNode){
		String askString = ""
				+ "ASK  { <" + resource.toString() + "> <" + property.toString() + "> <" + rdfNode.toString() + "> }";
		
		boolean result = executeAsk(model, askString);				
		
		return result;
	}
	
	protected static boolean executeAsk(OntModel model, String askString){
		Query query = QueryFactory.create(askString); 		
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		boolean result = qe.execAsk();
		return result;
	}
	
	public static ResultSet executeQuery(OntModel model, String queryString){
		if(!queryString.contains(PREFIXES)){
			queryString = PREFIXES + "\n" + queryString;
		}
		Query query = QueryFactory.create(queryString); 		
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		
		return results;
	}
	
	public static List<HashMap<String, String>> getResultValues(OntModel model, ResultSet results, String... vars){
		String modelPrefix = model.getNsPrefixURI("");
		List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();	
		while (results.hasNext()) 
		{	
			HashMap<String, String> resultRow = new HashMap<String, String>();
			QuerySolution row = results.next();
			boolean isFromModel = true;
			for (String var: vars) {
				String varValue = row.get(var).toString();
				if(!varValue.contains(modelPrefix) && !varValue.contains(MLT.getURI())){
					isFromModel = false;
					break;
				}
				resultRow.put(var, varValue);				
			}			
			if(isFromModel)
				result.add(resultRow);
		}
		return result;
	}	
}
