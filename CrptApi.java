import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int requestLimit;
    private AtomicInteger requestCount;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int period, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.scheduler = Executors.newScheduledThreadPool(1);

        // todo sync method requestCount.set(0) or not???
        scheduler.scheduleAtFixedRate(() -> requestCount.set(0), 0, period, timeUnit);

    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        synchronized (this) {
            while (requestCount.get() >= requestLimit) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted", e);
                }
            }
            System.out.println( requestCount.incrementAndGet() );
        }

        String jsonDocument = objectMapper.writeValueAsString(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        synchronized (this) {
            System.out.println( requestCount.decrementAndGet() );
            notifyAll();
        }
    }
}

public class Main {
    public static void main(String[] args) {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1, 10);

        Document document = new Document();

        Document.Description description = new Document.Description();
		// set JSON document to request
		...

        try {
            api.createDocument(document, "signature");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}




public class Document {
        // todo comment 'some json document fields...'; remove field to git not important
        public Description description;
		...
}
