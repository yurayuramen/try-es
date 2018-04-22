
# 概要

関連文書を探しだせる **more like this query** を試してみる

http://techlife.cookpad.com/entry/2014/09/24/092223


# インデックスの整備

## インデックスの削除

インデックスをリセットするために用意。
インデックスがない場合はエラーになるが、大きな問題はない

```bash
delete_index(){
  curl -XDELETE -H 'Content-Type: application/json' "http://localhost:9200/${1}?pretty"
}
delete_index try-mlt
```

## インデックスの設定

ダイナミックテンプレートを設定
３つのアナライザを適用

* kuromoji
https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-kuromoji-analyzer.html
* neologd 
https://engineering.linecorp.com/ja/blog/detail/109 
https://github.com/codelibs/elasticsearch-analysis-kuromoji-neologd 
http://christina04.hatenablog.com/entry/2016/05/18/193000
* ngram
https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-ngram-tokenizer.html

### コード

```bash
curl -XPUT -H 'Content-Type: application/json' http://localhost:9200/try-mlt?pretty -d '
{
  "settings": {
    "analysis": {
      "analyzer": {
        "kuromoji": {
          "type": "custom",
          "tokenizer": "kuromoji_tokenizer"
        },
        "ngram": {
          "tokenizer": "ngram"
        },
        "neologd":{
          "type": "custom",
          "tokenizer": "kuromoji_neologd_tokenizer"
        }
      },
      "tokenizer": {
        "ngram": {
          "type": "ngram",
          "min_gram": 3,
          "max_gram": 3,
          "token_chars": [
            "letter",
            "digit"
          ]
        }
      }
    }
  }
  ,
  "mappings": {
    "_doc": {
      "dynamic_templates": [
        {
          "strings": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type":  "keyword",
                  "ignore_above": "256"
                }
                ,"kuromoji": {
                  "type":  "text",
                  "analyzer": "kuromoji"
                }
                ,"neologd": {
                  "type":  "text",
                  "analyzer": "neologd"
                }
                ,"ngram": {
                  "type":  "text",
                  "analyzer": "ngram"
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



# データ投入

## 元ネタ取得

wikipediaから取得
wikipediaから取得する情報は

./try-es/src/main/resources/wikipedia.csv

より判断。

### wikipedia.csvの構成

#### 1列目は取得するコンテンツの種類

* pref
都道府県
* pref-capital
県庁所在地
* actor
俳優、女優。データは2017年人気女優ベスト10からピックアップ
https://www.channelcinema.com/ranking/2017actressjpbest.html
この情報はどれだけ信頼性のあるものなのかは？？？
* histrorival
歴史上の人物で、天下を他氏より奪った人をピックアップ

### コマンド

```bash
# wikipediaからHTMLデータをダウンロードして結果をgzファイルに保存
sbt "runMain tryes.main.TryESMain wikipedia"

# wikipediaからHTMLデータをダウンロードして結果をgzファイルに保存
set SBT_OPTS=-Xms256m -Xmx1024m
sbt "runMain tryes.main.TryESMain load"
```

# クエリー

* 関数定義

```
#
# 第一引数:関連記事を探したい対象のドキュメントID
# 第二引数:検索対象のフィールド
# 第三引数:max_query_terms
# 
more_like_this_ids(){
  #echo '###get###'
  #curl -XGET -H 'Content-Type: application/json' "http://localhost:9200/try-mlt/_doc/${1}"
  #start_time=`date +%s`
  #echo '###quey more like this###'
  curl -XPOST -H 'Content-Type: application/json' http://localhost:9200/try-mlt/_search?pretty -d "
  {
    \"query\":{
      \"more_like_this\" : {
          \"fields\" : [\"${2}\"],
          \"like\":[
            {
                \"_index\" : \"try-mlt\",
                \"_type\" : \"_doc\",
                \"_id\" : \"${1}\"
            }
          ],
          \"min_term_freq\" : 1,
          \"max_query_terms\" : ${3}
      }
    }
    ,\"size\": 3
    ,\"_source\": false
  }"
  end_time=`date +%s`
  time=$((end_time - start_time))
  #echo "${time} seconds"
}
```

* ここからが上記関数の呼び出しの例

```bash
more_like_this_ids pref-13 html 25
more_like_this_ids pref-13 html.kuromoji 25
more_like_this_ids pref-13 html.neologd 25
more_like_this_ids pref-13 html.ngram 25

more_like_this_ids pref-13 html 20000
more_like_this_ids pref-13 html.kuromoji 20000
more_like_this_ids pref-13 html.neologd 20000
more_like_this_ids pref-13 html.ngram 20000


more_like_this_ids pref-13 body 25
more_like_this_ids pref-13 body.kuromoji 25
more_like_this_ids pref-13 body.neologd 25
more_like_this_ids pref-13 body.ngram 25

more_like_this_ids pref-13 body_all_strip 25
more_like_this_ids pref-13 body_all_strip.kuromoji 25
more_like_this_ids pref-13 body_all_strip.neologd 25
more_like_this_ids pref-13 body_all_strip.ngram 25

more_like_this_ids pref-13 body_all_strip 20000
more_like_this_ids pref-13 body_all_strip.kuromoji 20000
more_like_this_ids pref-13 body_all_strip.neologd 20000
more_like_this_ids pref-13 body_all_strip.ngram 20000


more_like_this_ids historical-ashikaga-takauji body_all_strip 25000
more_like_this_ids historical-ashikaga-takauji body_all_strip.kuromoji 25000
more_like_this_ids historical-ashikaga-takauji body_all_strip.neologd 25000
more_like_this_ids historical-ashikaga-takauji body_all_strip.ngram 25000

more_like_this_ids historical-ashikaga-takauji body_all_strip 2500
more_like_this_ids historical-ashikaga-takauji body_all_strip.kuromoji 2500
more_like_this_ids historical-ashikaga-takauji body_all_strip.neologd 2500
more_like_this_ids historical-ashikaga-takauji body_all_strip.ngram 2500

more_like_this_ids historical-ashikaga-takauji body_all_strip 250
more_like_this_ids historical-ashikaga-takauji body_all_strip.kuromoji 250
more_like_this_ids historical-ashikaga-takauji body_all_strip.neologd 250
more_like_this_ids historical-ashikaga-takauji body_all_strip.ngram 250

more_like_this_ids historical-ashikaga-takauji body_all_strip 25
more_like_this_ids historical-ashikaga-takauji body_all_strip.kuromoji 25
more_like_this_ids historical-ashikaga-takauji body_all_strip.neologd 25
more_like_this_ids historical-ashikaga-takauji body_all_strip.ngram 25



more_like_this_ids historical-tokugawa-ieyasu body_all_strip 20000
more_like_this_ids historical-tokugawa-ieyasu body_all_strip.kuromoji 20000
more_like_this_ids historical-tokugawa-ieyasu body_all_strip.neologd 20000
more_like_this_ids historical-tokugawa-ieyasu body_all_strip.ngram 20000


```

# パラメータ

* max_query_terms

```text
選択されるクエリ用語の最大数。 
この値を大きくすると、クエリの実行速度を犠牲にして精度が向上します。 
デフォルトは25です。
```

* min_term_freq

```text
入力文書からその用語が無視される最小用語頻度。 
デフォルトは2です。
```

* min_freq_doc

```text
入力文書からその用語が無視される最小の文書頻度。 
デフォルトは5です。
```

* max_doc_freq

```text
入力文書からその用語が無視される最大文書頻度。 
これは、ストップワードなどの頻繁な単語を無視するために役立ちます。 
デフォルトは無制限（0）です。
```