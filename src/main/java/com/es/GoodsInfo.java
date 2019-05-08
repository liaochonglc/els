package com.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

@Document(indexName = "lctest",type = "goods"/*,shards = 1, replicas = 0*/)
//shards：分片数量，默认5
//replicas：副本数量，默认1
//indexName索引库名称 可以理解为数据库名 必须为小写 不然会报org.elasticsearch.indices.InvalidIndexNameException异常
//type类型 可以理解为表名
public class GoodsInfo implements Serializable {
    //标记一个字段为主键
    @Id
    private int id;
    @Field(type = FieldType.Text)
    private String name;

    public GoodsInfo(int id, String name, String description, Integer count) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Field(type = FieldType.Text)
    private String description;

    private Integer count;
//text：存储数据时候，会自动分词，并生成索引
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GoodsInfo(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "GoodsInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public GoodsInfo() {
    }
}
