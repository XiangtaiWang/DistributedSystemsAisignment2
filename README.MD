"mvn clean package" if need rebuild or compile proj

<br />Aggregation Server: clean history data every 5s, default port: 4567
format=> java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar AggregationServer {port}                    
<br />eg:<br />
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar AggregationServer                  
<br />or<br />
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar AggregationServer 4567                              

<br />Content Server: send update request to server every 5s               
format=> java ContentServer {address:port} {FilePath}               
eg.<br />
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar ContentServer 127.0.0.1:4567 SampleData               

<br />GetClient: send get request to server every 5s               
format=> java ContentServer {address:port} {FilePath}
<br />eg.<br />
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar GETClient 127.0.0.1:4567               
<br />or<br />
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar GETClient 127.0.0.1:4567 IDS60901               
<br />or<br />                
    java -cp target/Assignment2-1.0-SNAPSHOT-jar-with-dependencies.jar GETClient 127.0.0.1:4567 all                