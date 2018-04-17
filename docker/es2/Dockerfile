FROM elasticsearch:2.4.6-alpine

#COPY analysis-kuromoji-2.4.6.zip /root/kuromoji.zip
#RUN plugin install file:///root/kuromoji.zip
RUN plugin install analysis-kuromoji
RUN plugin install org.codelibs/elasticsearch-analysis-kuromoji-neologd/2.4.1

