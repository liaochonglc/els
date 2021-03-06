package com.es;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class GoodsController {
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
//query的时候，会先比较查询条件，然后计算分值，最后返回文档结果；
//而filter则是先判断是否满足查询条件，如果不满足，会缓存查询过程（记录该文档不满足结果）；
//满足的话，就直接缓存结果
//综上所述，filter快在两个方面：
//    1.对结果进行缓存
//    2.避免计算分值 这货有点像mysql的where
    //must 结构类似.bool -> must ->match
    //http://localhost:8888/save
    @RequestMapping("/save")
    public String save() {
        GoodsInfo goodsInfo = new GoodsInfo();
        for(int i = 15;i< 20 ; i++){
            goodsInfo.setId(i);
            goodsInfo.setCount(i);
            goodsInfo.setDescription("华为牛哦");
            goodsInfo.setName("华为真的牛逼");
            goodsRepository.save(goodsInfo);
        }
        return "success";
    }

    //http://localhost:8888/delete?id=1525415333329
    @GetMapping("delete")
    public String delete(int id) {
        GoodsInfo goodsInfo = new GoodsInfo();
        goodsInfo.setId(id);
        goodsRepository.delete(goodsInfo);
        return "success";
    }

    //http://localhost:8888/update?id=1525417362754&name=修改&description=修改
    @GetMapping("update")
    public String update(int id, String name, String description) {
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
        Pageable pageable = new PageRequest(0, 10);
        //支持模糊查询
        QueryBuilder queryBuilder = QueryBuilders.matchQuery("name", "华为");
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withPageable(pageable)
                .withQuery(queryBuilder).build();
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
        //查询关键字 BoolQueryBuilder和QueryBuilder的区别是 一个是链式 一个是单个
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name", "华为"));//下面可继续加query 组合查询
        //条件搜索
       /* searchQuery.withFilter(QueryBuilders.matchQuery("name","商品"));*/
        //排序
        searchQuery.withSort(SortBuilders.fieldSort("id").order(SortOrder.ASC));
        searchQuery.withQuery(boolQueryBuilder);
        //分页
        searchQuery.withPageable(PageRequest.of(0,5));
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
                   /* result.keySet().forEach(k->{
                        System.out.println("---------"+result.get(k));-
                        ----
                    });*/
                    String description = (String)result.get("description");
                    GoodsInfo goodsInfo = new GoodsInfo();
                    goodsInfo.setDescription(description);
                    goodsInfo.setId(Integer.parseInt(hit.getId()));
                    //获取name
                    HighlightField name = hit.getHighlightFields().get("name");
                    if(name!=null){
                        //获取高亮值
                        String name2 = name.getFragments()[0].toString();
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
    //springdata
    @RequestMapping("/getCount")
    public void getCount(){
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchAllQuery());
        // 聚合查询。goodsSales是要统计的字段，sum_sales是自定义的别名
        TermsAggregationBuilder field = AggregationBuilders.terms("id_s").field("id");
        SumAggregationBuilder sumBuilder = AggregationBuilders.sum("sum").field("count");
        field.subAggregation(sumBuilder);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .addAggregation(sumBuilder)
                .build();

        double saleAmount = elasticsearchTemplate.query(searchQuery, response -> {
            InternalSum sum = (InternalSum)response.getAggregations().asList().get(0);
            return sum.getValue();
        });
        System.out.println(saleAmount);
    }
    //这是原生的方式
    //field_value_factor
    @RequestMapping("/getScore")
    public void getScore(){
        SearchRequestBuilder searchRequestBuilder = elasticsearchTemplate.getClient().prepareSearch("lctest").setTypes("goods");
        BoolQueryBuilder should = QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("name", "真的牛逼"));
        FieldValueFactorFunctionBuilder fieldQuery = new FieldValueFactorFunctionBuilder("count");
        // 额外分数=log(1+score)
        fieldQuery.factor(0.1f);
        fieldQuery.modifier(FieldValueFactorFunction.Modifier.LOG1P);
        // 最终分数=_score+额外分数
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders
                .functionScoreQuery(should, fieldQuery)
                //限制加强函数的最大效果
                .boostMode(CombineFunction.SUM);
        //根据分值倒序排列 这个字段是固定的
        searchRequestBuilder.addSort("_score", SortOrder.ASC);
        searchRequestBuilder.setQuery(functionScoreQueryBuilder);
        //设置获取位置个数
        searchRequestBuilder.setFrom(0).setSize(100);
        //设置查询分片,防止震荡问题,生产中可以用 用户id 填入,对同一个用户保证多次查询结果相同
        searchRequestBuilder.setPreference("233");
        SearchResponse searchResponse = searchRequestBuilder.get();
        SearchHits hits = searchResponse.getHits();
        for(SearchHit h:hits){
            float score = h.getScore();
            System.out.println(score);
            Map<String, Object> sourceAsMap = h.getSourceAsMap();
            System.out.println(sourceAsMap);
        }
    }

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
