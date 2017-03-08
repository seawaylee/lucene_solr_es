package lucene;


import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

/**
 * @author NikoBelic
 * @create 07/03/2017 13:05
 */
public class LuceneTest
{
    /**
     * 创建索引
     *
     * @Author NikoBelic
     * @Date 07/03/2017 19:01
     */
    @Test
    public void testIndexCreate() throws IOException
    {
        // 指定文档和索引的存储目录
        Directory indexDir = FSDirectory.open(new File("/Users/lixiwei-mac/Documents/DataSet/lucene/index"));

        // 标准分词器(英文效果好,中文单字分词)
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(indexDir, config);

        // 采集文档中的数据放入Lucene中
        File sourceDir = new File("/Users/lixiwei-mac/Documents/DataSet/lucene/searchsource");
        for (File f : sourceDir.listFiles())
        {
            String fileName = f.getName();
            String fileContent = FileUtils.readFileToString(f);
            String filePath = f.getPath();
            long fileSize = FileUtils.sizeOf(f);

            // (field_name,field_value,need_stored?)
            Field fileNameField = new TextField("filename", fileName, Field.Store.YES);
            Field fileContentField = new TextField("content", fileContent, Field.Store.NO);
            Field filepPthField = new StoredField("path", filePath);
            Field fileSizeField = new LongField("size", fileSize, Field.Store.YES);
            Document document = new Document();

            document.add(fileNameField);
            document.add(fileContentField);
            document.add(filepPthField);
            document.add(fileSizeField);

            // 这里会自动创建索引
            indexWriter.addDocument(document);
        }
        indexWriter.close();
    }

    /**
     * 使用索引搜索
     *
     * @Author NikoBelic
     * @Date 07/03/2017 19:01
     */
    @Test
    public void testIndexSearch() throws IOException, ParseException
    {
        // 指定索引库存放路径
        Directory indexDir = FSDirectory.open(new File("/Users/lixiwei-mac/Documents/DataSet/lucene/index"));
        // 创建索引Reader、Searcher对象
        IndexReader indexReader = DirectoryReader.open(indexDir);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        // 创建查询
        // 方法1
        Query query = new TermQuery(new Term("content", "java"));
        // 执行查询,(查询对象,查询结果返回最大值)


        // 方法2
        // 创建分词器(必须和创建索引所用分词器一致)
        Analyzer analyzer = new IKAnalyzer();
        // 默认搜索域作用:如果搜索语法中没有指定域名,则使用默认域名搜索
        QueryParser queryParser = new QueryParser("filename", analyzer);
        // 查询语法:域名:搜索关键字
        Query query2 = queryParser.parse("apache");


        TopDocs topDocs = indexSearcher.search(query2, 5);
        System.out.println("查询结果的总条数:" + topDocs.totalHits);

        // 遍历查询结果
        for (ScoreDoc scoreDoc : topDocs.scoreDocs)
        {
            // scoreDoc.doc = 自动生成的文档ID
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("filename"));
            System.out.println(scoreDoc.toString());
            System.out.println("======================================================");
        }
        indexReader.close();
    }

    @Test
    public void testDelIndex() throws IOException
    {
        Analyzer analyzer = new IKAnalyzer();
        Directory indexDir = FSDirectory.open(new File("/Users/lixiwei-mac/Documents/DataSet/lucene/index"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(indexDir, config);

        // 删除所有
        //indexWriter.deleteAll();
        // Term 词源,(域名,删除含有这些关键词的数据)
        indexWriter.deleteDocuments(new Term("filename", "apache"));
        indexWriter.commit();
        indexWriter.close();
    }

    /**
     * 更新就是按照传入的Term进行搜索,如果找到结果那么删除,将更新的内容重新生成一个Document对象
     * 如果没有搜索到结果,那么将更新的内容直接添加一个新的Document对象
     *
     * @Author NikoBelic
     * @Date 07/03/2017 20:57
     */
    @Test
    public void testUpdateIndex() throws IOException
    {
        Analyzer analyzer = new IKAnalyzer();
        Directory indexDir = FSDirectory.open(new File("/Users/lixiwei-mac/Documents/DataSet/lucene/index"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(indexDir, config);

        Document doc = new Document();
        doc.add(new TextField("filename", "更新检索测试.txt", Field.Store.YES));
        doc.add(new TextField("content", "文件内容测试", Field.Store.NO));
        doc.add(new LongField("size", 100L, Field.Store.YES));


        indexWriter.updateDocument(new Term("filename", "检索"), doc);

        indexWriter.commit();
        indexWriter.close();
    }

    /**
     * 根据索引查询,多种查询
     * @Author NikoBelic
     * @Date 08/03/2017 13:02
     */
    @Test
    public void testSearch() throws IOException, ParseException
    {
        Directory indexDir = FSDirectory.open(new File("/Users/lixiwei-mac/Documents/DataSet/lucene/index"));
        IndexReader indexReader = DirectoryReader.open(indexDir);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        // 根据文本查询
        Query termQuery = new TermQuery(new Term("filename", "apache"));
        // 根据数字范围查询
        Query numQuery = NumericRangeQuery.newLongRange("size", 100L, 800L, true, true);
        // Bool查询
        BooleanQuery boolQuery = new BooleanQuery();
        boolQuery.add(termQuery, BooleanClause.Occur.MUST); // 独自使用MUST_NOT没有任何意义
        boolQuery.add(numQuery, BooleanClause.Occur.MUST);

        // 查询所有文档
        MatchAllDocsQuery matchAllDocsQuery = new MatchAllDocsQuery();

        // 多个域的查询,或 关系
        String[] fields = {"filename", "content"};
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, new IKAnalyzer());
        Query multiFieldQuery = multiFieldQueryParser.parse("apache");


        //TopDocs topDocs = indexSearcher.search(boolQuery, 10);
        //TopDocs topDocs = indexSearcher.search(matchAllDocsQuery, 10);
        TopDocs topDocs = indexSearcher.search(multiFieldQuery, 10);
        System.out.println("符合条件的文档数:" + topDocs.totalHits);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs)
        {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("filename"));
            System.out.println(document.get("size"));
            System.out.println("======================================================================");
        }

    }


}
