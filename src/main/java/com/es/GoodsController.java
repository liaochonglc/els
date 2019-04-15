package com.es;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;

@RestController
public class GoodsController {
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    //http://localhost:8888/save
    @RequestMapping("/save")
    public String save() {
        GoodsInfo goodsInfo = new GoodsInfo(System.currentTimeMillis(),
                "商品" + System.currentTimeMillis(), "这是十一个测试商品");
        goodsRepository.save(goodsInfo);
        return "success";
    }

    //http://localhost:8888/delete?id=1525415333329
    @GetMapping("delete")
    public String delete(long id) {
        GoodsInfo goodsInfo = new GoodsInfo();
        goodsInfo.setId(id);
        goodsRepository.delete(goodsInfo);
        return "success";
    }

    //http://localhost:8888/update?id=1525417362754&name=修改&description=修改
    @GetMapping("update")
    public String update(long id, String name, String description) {
        GoodsInfo goodsInfo = new GoodsInfo(id,
                name, description);
        goodsRepository.save(goodsInfo);
        return "success";
    }

    //http://localhost:8888/getOne?id=1525417362754
    @GetMapping("getOne")
    public GoodsInfo getOne(long id) {
        Optional<GoodsInfo> byId = goodsRepository.findById(id);
        GoodsInfo goodsInfo = byId.get();
        return goodsInfo;
    }

    //继承方式不支持高亮
    @RequestMapping("/getList")
    public List<GoodsInfo> getGoodsList() {
        List<GoodsInfo> list = new ArrayList<>();
        Pageable pageable = new PageRequest(0, 5);
        QueryStringQueryBuilder builder = new QueryStringQueryBuilder("商品");
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withPageable(pageable)
                .withQuery(builder).build();
        Iterable<GoodsInfo> search = goodsRepository.search(searchQuery);
        Iterator<GoodsInfo> iterator = search.iterator();
        while (iterator.hasNext()) {
            /*System.out.println(iterator.next().getDescription());*/
            GoodsInfo next = iterator.next();
            list.add(next);
        }
        return list;
    }

    @RequestMapping("/getColorList")
    public List<GoodsInfo> getColrList() {
        NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder();
        //查询关键字
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name", "商品"));//下面可继续加query 组合查询
        //条件搜索
        searchQuery.withFilter(QueryBuilders.matchQuery("name","商品"));
        //排序
        searchQuery.withSort(SortBuilders.fieldSort("id").order(SortOrder.ASC));
        searchQuery.withQuery(boolQueryBuilder);
        //分页
        searchQuery.withPageable(PageRequest.of(0,10));
        searchQuery.withHighlightFields(new HighlightBuilder.Field("name").preTags("<font color='red'>").postTags("</font>"));
        Page<GoodsInfo> page = elasticsearchTemplate.queryForPage(searchQuery.build(), GoodsInfo.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                List<GoodsInfo> poems = new ArrayList<GoodsInfo>();
                SearchHits hits = searchResponse.getHits();
                //获取命中文本结果
                //循环命中集合
                for (SearchHit hit : hits) {
                    //获取结果数据
                    Map<String, Object> result = hit.getSourceAsMap();
                    GoodsInfo goodsInfo = new GoodsInfo();
                    goodsInfo.setId(Long.parseLong(hit.getId()));
                    //获取name
                    HighlightField name = hit.getHighlightFields().get("name");
                    if(name!=null){
                        //获取高亮值
                        String name2 = name.getFragments()[0].toString();
                        System.out.println("高亮字段："+name);
                        goodsInfo.setName(name2);
                    }
                    poems.add(goodsInfo);
                }
                //返回
                if(poems!=null && poems.size()>0){
                    return new AggregatedPageImpl(poems);
                }
                return null;
            }
        });
        List<GoodsInfo> poems = page.getContent();
        return poems;
    }
    /*//每页数量
    private Integer PAGESIZE=10;

    //http://localhost:8888/getGoodsList?query=商品
    //http://localhost:8888/getGoodsList?query=商品&pageNumber=1
    //根据关键字"商品"去查询列表，name或者description包含的都查询
    @GetMapping("getGoodsList")
    public List<GoodsInfo> getList(Integer pageNumber, String query){
        if(pageNumber==null) pageNumber = 0;
        //es搜索默认第一页页码是0
        SearchQuery searchQuery=getEntitySearchQuery(pageNumber,PAGESIZE,query);
        Page<GoodsInfo> goodsPage = goodsRepository.search(searchQuery);
        return goodsPage.getContent();
    }*/

/*
    private SearchQuery getEntitySearchQuery(int pageNumber, int pageSize, String searchContent) {
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery()
                .add(QueryBuilders.matchPhraseQuery("name", searchContent),
                        ScoreFunctionBuilders.weightFactorFunction(100))
                .add(QueryBuilders.matchPhraseQuery("description", searchContent),
                        ScoreFunctionBuilders.weightFactorFunction(100))
                //设置权重分 求和模式
                .scoreMode("sum")
                //设置权重分最低分
                .setMinScore(10);
        // 设置分页
        Pageable pageable = new PageRequest(pageNumber, pageSize);
        return new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(functionScoreQueryBuilder).build();
    }*/
}
