import java.util.ArrayList;
import java.util.Date;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class SparqlUtil {
	static public String PREFIXES = ""
			+ "PREFIX owl: <" + OWL.getURI() + ">\n"
			+ "PREFIX rdfs: <" + RDFS.getURI() + ">\n"
			+ "PREFIX rdf: <" + RDF.getURI() + ">\n"
			+ "PREFIX xml: <" + XSD.getURI() + ">\n";
		
	public static ArrayList<String> getInstancesOf(Model model, String classUri){
		String queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT *\n"
				+ "WHERE {\n"
				+ "	?instance rdf:type <" + classUri + "> . \n"
				+ "}";
		ResultSet results = executeQuery(model, queryString);

		ArrayList<String> allInstances = new ArrayList<String>();
		while (results.hasNext()){
			QuerySolution row = results.next();
			String instance = row.get("instance").toString();
			allInstances.add(instance);	
		}
		return allInstances;
	}
	
	/**
	 * This function build a SPARQL query to get all classes
	 * i.e., use the predicate rdf:type to get instances of owl:Class
	 * and then return a list of all classes
	 */
	public static ArrayList<String> getAllClasses(Model model){
		String queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT *\n"
				+ "WHERE {\n"
				+ "	?class rdf:type owl:Class .  \n"
				+ "}";
		ResultSet results = executeQuery(model, queryString);

		ArrayList<String> allClasses = new ArrayList<String>();
		while (results.hasNext()){
			QuerySolution row = results.next();
			String classUri = row.get("class").toString();
			allClasses.add(classUri);
		}
		
		return allClasses;
	}
	
	/**
	 * This function build a SPARQL query to get all subclass of a given class
	 * i.e., use the predicate rdfs:subClassOf
	 * 
	 * A second query is build to cover cases that a subclassing can be inferred.
	 * This kind of subclassing is defined as 
	 * A given class A is equivalent to an intersection, and inside this intersection exist other class B
	 * Then, we can infer that A is subclass of B.
	 * In the camera.owl, it is the cases of BodyWithNonAdjustableShutterSpeed and SLR
	 * 
	 * And then return a list of all subclasses
	 */
	public static ArrayList<String> getAllSubClasses(Model model, String classUri){
		String queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT ?subClass \n"
				+ "WHERE {\n"
				+ "	?subClass rdfs:subClassOf <" + classUri + "> .  \n"
				+ "}";
		ResultSet results = executeQuery(model, queryString);

		ArrayList<String> allSubClasses = new ArrayList<String>();
		while (results.hasNext()){
			QuerySolution row = results.next();
			String subClassUri = row.get("subClass").toString();
			allSubClasses.add(subClassUri);
		}		
		
		queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT ?subClass \n"
				+ "WHERE {\n"
				+ "	?list rdf:rest*/rdf:first <" + classUri + "> .\n"
				+ "	?equivClass owl:intersectionOf ?list .\n"
				+ "	?subClass owl:equivalentClass ?equivClass .  \n"
				+ "}";
		results = executeQuery(model, queryString);
		while (results.hasNext()){
			QuerySolution row = results.next();
			String subClassUri = row.get("subClass").toString();
			if(allSubClasses.contains(subClassUri)) continue;
			allSubClasses.add(subClassUri);
		}	
		
		return allSubClasses;
	}
	
	
	/**
	 * This function build a SPARQL query to get distinct properties that have at least one subproperty
	 * i.e., use the predicate rdfs:subPropertyOf
	 * and then return a list of all the properties
	 */
	public static ArrayList<String> getAllPropWithSub(Model model){
		String queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT ?property \n"
				+ "WHERE {\n"
				+ "	?subProperty rdfs:subPropertyOf ?property .  \n"
				+ "}";
		ResultSet results = executeQuery(model, queryString);

		ArrayList<String> allPropWithSub = new ArrayList<String>();
		while (results.hasNext()){
			QuerySolution row = results.next();
			String propertyUri = row.get("property").toString();
			
			allPropWithSub.add(propertyUri);
		}		
		
		return allPropWithSub;
	}	
	
	/**
	 * This function build a SPARQL query to get properties and their domain and ranges
	 * i.e., first get the union of instances of owl:ObjectProperty and owl:DatatypeProperty
	 * then, optionally get the domain and the range of this property
	 * The use of OPTIONAL becomes necessary for properties without definition of domain and range
	 * It is the case of camera:part
	 * and then return a list of all the properties with their domain and ranges
	 */
	public static ArrayList<ArrayList<String>> getAllDomainAndRanges(Model model){
		String queryString = ""
				+ PREFIXES
				+ "SELECT DISTINCT * \n"
				+ "WHERE {\n"
				+ "	{\n"
				+ "		?property rdf:type owl:ObjectProperty .\n"
				+ "	}UNION{\n"
				+ "		?property rdf:type owl:DatatypeProperty .\n"
				+ "	}\n"
				+ "	OPTIONAL{\n"
				+ "		?property rdfs:domain ?domain .\n"
				+ "		?property rdfs:range ?range .\n"
				+ "	}\n"
				+ "}";
		ResultSet results = executeQuery(model, queryString);

		ArrayList<ArrayList<String>> allDomainAndRanges = new ArrayList<ArrayList<String>>();
		while (results.hasNext()){
			QuerySolution row = results.next();
			String propertyUri = row.get("property").toString();
			String domainUri = "";
			if(row.get("domain") != null) domainUri = row.get("domain").toString();
			String rangeUri = "";
			if(row.get("range") != null) rangeUri = row.get("range").toString();
			
//			if(!propertyUri.contains(cameraUri)) continue;
//			if(!domainUri.contains(cameraUri) && !domainUri.equals("") && !domainUri.contains(XSD.getURI())) continue;
//			if(!rangeUri.contains(cameraUri) && !rangeUri.equals("") && !rangeUri.contains(XSD.getURI())) continue;
			
			ArrayList<String> triple = new ArrayList<String>();
			triple.add(propertyUri);
			triple.add(domainUri);
			triple.add(rangeUri);
			
			allDomainAndRanges.add(triple);
		}		
		
		return allDomainAndRanges;
	}	
	
	/**
	 * This function create the query and execute it at the model
	 * and then return the result set
	 */
	protected static ResultSet executeQuery(Model model, String queryString){
		Query query = QueryFactory.create(queryString); 		
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		
		return results;
	}
	
	/**
	 * This function create the query and execute it at a external sparql endpoint
	 * and then return the result set
	 * @throws Exception 
	 */
	public static ResultSet externalQuery (String query, String serviceURL){
		boolean error = false;
		do {
			error = false;
			try {
				QueryExecution queryExecution = QueryExecutionFactory.sparqlService(serviceURL, query);	
				ResultSet results = queryExecution.execSelect();		
				return results;
			} catch (Exception e) {				
				error = true;
				Date begin = new Date();
				System.out.println("Perda de Conex�o em " + begin);
				long diff = 0;
				int waitFor = 6000000;
				Thread t = new Thread();
				try {
					t.wait(waitFor);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				do {
					diff = getExecutionTimeInMs(begin);
					System.out.print("");
				} while (diff >= waitFor);
				Date comingBack = new Date();
				System.out.println("Retornando em " + comingBack);
			}
			
		} while (error);
		return null;
	}
	
	public static boolean externalAsk (String askString, String serviceURL){
		boolean error = false;
		do {
			error = false;
			try {
				Query query = QueryFactory.create(askString); 		
				QueryExecution qe = QueryExecutionFactory.sparqlService(serviceURL, askString);
				boolean result = qe.execAsk();
				return result;
			} catch (Exception e) {				
				error = true;
				Date begin = new Date();
				System.out.println("Perda de Conex�o em " + begin);
				long diff = 0;
				int waitFor = 6000000;
				Thread t = new Thread();
				try {
					t.wait(waitFor);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				do {
					diff = getExecutionTimeInMs(begin);
					System.out.print("");
				} while (diff >= waitFor);
				Date comingBack = new Date();
				System.out.println("Retornando em " + comingBack);
			}
			
		} while (error);
		return false;
	}
		
	public static long getExecutionTimeInMs(Date beginDate){
		Date endDate = new Date();
		long diff = endDate.getTime() - beginDate.getTime();
		return diff;
	}
}
