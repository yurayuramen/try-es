
# 概要

kuromojiアナライザをいれたときといれてないときの違いをわかりやすくする

インデックス **real-world** はkuromojiアナライザを設定しない
インデックス **another-world** kuromojiはアナライザを設定する


# インデックスの整備

## インデックスの削除

インデックスをリセットするために用意。
インデックスがない場合はエラーになるが、大きな問題はない

```bash
delete_index(){
  curl -XDELETE -H 'Content-Type: application/json' "http://localhost:9200/${1}?pretty"
}

delete_index real-world
delete_index another-world
```

## インデックスの設定変更

### Elasticsearch2系

* インデックス **real-world** のアナライザはデフォルトのまま

```bash
curl -XPUT -H 'Content-Type: application/json' http://localhost:9200/real-world?pretty -d '
{
  "mappings": {
    "_all": {
      "dynamic_templates": [
        {
          "strings": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "string",
              "index": "analyzed",
              "fields": {
                "keyword": {
                  "type":  "string",
                  "index": "not_analyzed"
                }
              }
            }
          }
        }
      ]
    }
  }
}
'
```

* インデックス **another-world** のアナライザはkuromojiに変更

```bash
curl -XPUT -H 'Content-Type: application/json' http://localhost:9200/another-world?pretty -d '
{
  "mappings": {
    "self": {
      "dynamic_templates": [
        {
          "strings": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "string",
              "fields": {
                "keyword": {
                  "type":  "string",
                  "index": "not_analyzed"
                }
              }
            }
          }
        }
      ]
    }
  },
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "custom",
          "tokenizer": "kuromoji_tokenizer"
        }
      }
    }
  }
}
'
```

### Elasticsearch6系

この設定は、インデックス **another-world**にのみ適用 
elasticsearch5からdefaultのdynamic templateが変わったので、独自のdynamic tenplateを作成する必要がなくなった

https://www.elastic.co/jp/blog/strings-are-dead-long-live-strings
https://mozami.me/2017/06/03/elasticsearch_kibana5_template.html

```bash
curl -XPUT -H 'Content-Type: application/json' http://localhost:9200/another-world?pretty -d '
{
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "custom",
          "tokenizer": "kuromoji_tokenizer"
        }
      }
    }
  }
}
'
```


### 補足

elasticsearch2.xの頃はelasticsearch.ymlでデフォルトのアナライザを指定することはできたが、
elasticsearch5.xからはelasticsearch.ymlではnodeに関する設定しかできなくなった。

* 2系までの設定
https://medium.com/hello-elasticsearch/elasticsearch-91c9f22c663a
https://qiita.com/mserizawa/items/8335d39cacb87f12b678

* 5以降は使用不可になった
http://www.84kure.com/blog/2017/07/19/


## データ投入

### １件ごとに投入

* URIは /インデックス名/タイプ名/ドキュメントID
* Httpメソッドは PUT
* 例

```bash
put_doc(){
curl -XPUT -H 'Content-Type: application/json' http://localhost:9200/${1}/self/kimura?pretty -d '
{"name": "藤原", "address": "京都市中京区西ノ京桑原町", "tel": "075-222-9999"}
'
}

put_doc real-world
put_doc another-world

```

### バルク（まとめて）投入

* URIは /インデックス名/タイプ名/ドキュメントID
* Httpメソッドは POST
* HttpBodyの構造は以下を参考に
https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
* 例

```bash
bulk_docs(){

  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/self/_bulk?pretty -d '
{ "index": { "_id": "yamada" }}
{"name": "山田", "address": "川崎市中原区小杉陣屋町", "tel": "044-7272-1234"}
{ "index": { "_id": "tanaka" }}
{"name": "田中", "address": "川崎市中原区小杉御殿町", "tel": "044-7272-9999"}
{ "index": { "_id": "sato" }}
{"name": "佐藤", "address": "杉並区永福", "tel": "03-5313-9999"}
{ "index": { "_id": "kimura" }}
{"name": "鈴木", "address": "多可郡多可町中区中村町", "tel": "078-995-1234"}
'
}

bulk_docs real-world
bulk_docs another-world


```

# インデックスの情報を確認

```bash
show_index(){
  echo '###settings###'
  curl -XGET -H 'Content-Type: application/json' http://localhost:9200/${1}/_settings?pretty
  echo '###mappings###'
  curl -XGET -H 'Content-Type: application/json' http://localhost:9200/${1}/_mappings?pretty
  echo '###search###'
  curl -XGET -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty
}

show_index real-world
show_index another-world

```


# クエリー

## 氏名を検索

```bash
query_name(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty -d "
  {
      \"query\": {
          \"match\" : {
              \"name\" : \"${2}\"
          }
      }
      ,\"explain\": true
  }
  "
}

query_name real-world "山田"
query_name another-world "山田"

```

### 何が違うのかみてみよう analyze API

https://www.elastic.co/guide/en/elasticsearch/reference/6.2/indices-analyze.html

```bash
show_explain(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_analyze?pretty -d "
  {
    \"text\" : \"${2}\",
    \"explain\": true
  }
  "
}

show_explain real-world 山田
show_explain another-world 山田

```

## 住所を検索

analyze APIを合わせて呼び出してみる

```bash
query_address(){
  echo '#####query#####'
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty -d "
  {
      \"query\": {
          \"match\" : {
              \"address\" : \"${2}\"
          }
      }
  }
  "
  echo '#####explain#####'
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_analyze?pretty -d "
  {
    \"text\" : \"${2}\",
    \"explain\": true
  }
  "
}
query_address real-world 川崎中原
query_address another-world 川崎中原

```


### ちょっと違う方法で検索(TermQuery)

https://medium.com/veltra-engineering/elasticsearch-fulltext-termlevel-772e8a9152b1

何もヒットしない
こちらはどちらのインデックスでも同じ結果になる

```
query_address_term(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty -d "
  { 
    \"query\": {
      \"term\" : { \"address\" : \"${2}\" }
    }
  }
  "
}
query_address_term real-world 川崎中原
query_address_term another-world 川崎中原
```

## 住所をちょっと違う角度で検索(Prefix)

not_analyzedな（マルチ）フィールドに対してクエリーを投げる
こちらはどちらのインデックスでも同じ結果になる

```bash

query_address_prefix(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty -d "
  { 
    \"query\": {
      \"prefix\" : { \"address.keyword\": \"${2}\" }
    }
  }
  "
}
query_address_prefix real-world "川崎市中原区小杉"
query_address_prefix another-world 川崎市中原区小杉

```


# 余談

## 同じフィールドに複数件のデータを投入

```bash
bulk_docs2(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/self/_bulk?pretty -d '
  { "index": { "_id": "x01" }}
  {"name": "中垣内", "address": ["川崎市中原区小杉町","大阪市北区梅田"] , "tel": "044-7272-1234"}
  { "index": { "_id": "x02" }}
  {"name": "大河内", "address": ["川崎市中原区小杉町","東京都千代田区大手町"], "tel": "044-7272-9999"}
  { "index": { "_id": "x03" }}
  {"name": "権田原", "address": ["杉並区永福","福岡市中央区天神"], "tel": "03-5313-9999"}
  { "index": { "_id": "x04" }}
  {"name": "山之内", "address": ["神戸市西区木見","東京都千代田区日比谷"], "tel": "078-994-7000"}
  '
}
bulk_docs2 real-world
bulk_docs2 another-world

```

## 複合検索

bool queryを使うと可能

https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html
https://www.elastic.co/guide/en/elasticsearch/reference/2.3/query-dsl-bool-query.html
https://qiita.com/vanhuyz/items/04a6871ae5f53ba5a97f

must ... and相当 / score計算に影響する
filter ... and相当 / score計算に影響しない
should ... or相当
must_not ... not in相当

### AND

```bash
query_and(){
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${1}/_search?pretty -d "
  {
      \"query\": {
          \"bool\":{
              \"must\":[
                  {\"match\" : {\"address\" : \"${2}\"}},
                  {\"match\" : {\"address\" : \"${3}\"}}
              ]
          }
      }
  }
  "
}
query_and real-world "千代田" "川崎中原"
query_and another-world "千代田" "川崎中原"
```

### OR

```bash
curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/${INDEX_NM}/_search?pretty -d '
{
    "query": {
        "bool":{
            "should":[
                {"match" : {"address" : "千代田"}},
                {"match" : {"address" : "川崎中原"}}
            ]
        }
    }
}
'
```
