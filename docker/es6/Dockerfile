FROM docker.elastic.co/elasticsearch/elasticsearch:6.2.1
#COPY analysis-kuromoji-6.2.1.zip /root/kuromoji.zip
#RUN elasticsearch-plugin install file:///root/kuromoji.zip
RUN elasticsearch-plugin install  analysis-kuromoji
RUN elasticsearch-plugin install org.codelibs:elasticsearch-analysis-kuromoji-neologd:6.2.1
