import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelG;
import org.apache.lucene.search.similarities.BasicModelIF;
import org.apache.lucene.search.similarities.BasicModelIn;
import org.apache.lucene.search.similarities.BasicModelIne;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.Normalization;
import org.apache.lucene.search.similarities.NormalizationH1;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.NormalizationH3;
import org.apache.lucene.search.similarities.NormalizationZ;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class lucene_seach_2 {	
	public static void main(String[] args) throws IOException, ParseException, org.json.simple.parser.ParseException {
		// Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer();

		// Create the index
		Directory index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig( analyzer);
		
		//uncomment these lines for different search algorithms
		config.setSimilarity(new BM25Similarity());
//		config.setSimilarity(new ClassicSimilarity());
//		config.setSimilarity(new DFRSimilarity(new BasicModelIn(), new AfterEffectB(), new NormalizationZ()));
//		config.setSimilarity(new DFRSimilarity(new BasicModelIne(), new AfterEffectB(), new NormalizationH1()));
		
		Hashtable<String, String> doc_descs= new Hashtable<String, String>();
		read_docs(doc_descs);
	    	    
		IndexWriter w = new IndexWriter(index, config);
		for (String did: doc_descs.keySet()) {
			addDoc(w, doc_descs.get(did), did);
		}
//		addDoc(w, "Lucene in Action", "193398817");
//		addDoc(w, "Lucene for Dummies", "55320055Z");
//		addDoc(w, "Managing Gigabytes", "55063554A");
//		addDoc(w, "The Art of Computer Science", "9900333X");
		w.close();

		

		
		
		
		// Query, 612729
		Hashtable<String, String> query_descs = new Hashtable<String, String>();
		Hashtable<String, Integer> query_index = new Hashtable<String, Integer>();
		read_queries(query_descs, query_index);
		int cnt=0;
		BufferedWriter bw= new BufferedWriter(new FileWriter("run.txt"));
		for (String qid : query_descs.keySet()) {
			String querystr = args.length > 0 ? args[0] : query_descs.get(qid);
//			System.out.println(querystr.split(" ").length + "\t" + querystr);

			// the "title" arg specifies the default field to use
			// when no field is explicitly specified in the query.
			Query q = new QueryParser("title", analyzer).parse(querystr);

			
			// Search
			int hitsPerPage = doc_descs.size(); //1000
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(new BM25Similarity());
//			searcher.setSimilarity(new ClassicSimilarity());
//			searcher.setSimilarity(new DFRSimilarity(new BasicModelIne(), new AfterEffectB(), new NormalizationH1()));
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, Integer.MAX_VALUE); // doc_descs.size());
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// Display results
			System.out.println(cnt + " found " + hits.length + " hits.");
			cnt+=1;
			for (int i = 0; i < hits.length; ++i) {
				int did = hits[i].doc;
				float d_score = hits[i].score;
				Document d = searcher.doc(did);
				bw.write(query_index.get(qid) + " Q0 " + d.get("isbn") + " " + i + " " + d_score + " run "+qid+"\n");
			}
			reader.close();
		}
		bw.close();
	}

	private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("title", title, Field.Store.YES));

		// use a string field for isbn because we don't want it tokenized
		doc.add(new StringField("isbn", isbn, Field.Store.YES));
		w.addDocument(doc);
	}
	
	
	//update this function to read docs.. the format of each line should be: did TAB text_description 
	private static void read_docs(Hashtable<String, String> doc_descs){
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("genes.txt"));
			String st = "";
			while ((st = br.readLine()) != null) {
//				System.out.println(st);
				String [] sp = st.split("\t");
				String did = sp[0];
				String desc = st.substring(did.length(), st.length()).trim();
//				System.out.println(desc);
				doc_descs.put(did, desc);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//update this function to read queries.. the format of each line should be: qid TAB relevant_doc_ids(comma_seperated) TAB query_text_description 
	private static void read_queries(Hashtable<String, String> query_descs, Hashtable<String, Integer> query_index){
		BufferedReader br;
		BufferedWriter bw;
		try {
			br = new BufferedReader(new FileReader("test_phenotypes.txt"));
			bw = new BufferedWriter(new FileWriter("qrel.txt"));
			String st = "";
			int ind = 0;
			while ((st = br.readLine()) != null) {
//				System.out.println(st);
				String[] sp = st.split("\t");
				String qid = sp[0];
				String dids = sp[1];
				
				String desc = st.substring(qid.length() + 1 + dids.length() + 1, st.length()).trim();
				query_descs.put(qid, desc);
				query_index.put(qid, ind);
				
				for (String did : dids.split(",")) {
					bw.write(ind + " 0 " + did + " 1\n");
				}
				ind += 1;
			}
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
	
	

