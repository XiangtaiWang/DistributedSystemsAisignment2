import com.fasterxml.jackson.databind.ObjectMapper;

class RequestHandler {
    private final HistoryFileHandler historyFileHandler;

    public RequestHandler(HistoryFileHandler historyFileHandlerDI) {
        historyFileHandler = historyFileHandlerDI;
    }

    public String HandleRequest(String request) {
        String[] split = request.split("\n");

        String response;
        if (split[0].startsWith("PUT")) {
            System.out.println("Received PUT request");
            String body = split[split.length - 1];
            response = ProcessUpdateWeatherRequest(body);
        } else if (split[0].startsWith("GET")) {
            System.out.println("Received PUT request");
            response = GetWeatherRequest();
        } else {
            response = BadRequest();
        }

        return response;
    }

    private String BadRequest() {
        return CreateResponse(HttpStatus.HTTP_BAD_REQUEST, "{}");
    }

    private String CreateResponse(HttpStatus httpStatus, String body) {
        int code;
        switch (httpStatus) {
            case HTTP_CREATED:
                code = 201;
                break;
            case HTTP_SUCCESS:
                code = 200;
                break;
            case HTTP_BAD_REQUEST:
                code = 400;
                break;
            case HTTP_INTERNAL_ERROR:
                code = 500;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + httpStatus);
        }
        String status = code + " " + httpStatus;
        String httpVersion = "HTTP/1.1";
        String contentType = "application/json";
        int contentLength = body.getBytes().length;


        String response = httpVersion +
                " " +
                status +
                "\r\n" +
                "Content-Type: " +
                contentType +
                "\r\n" +
                "Content-Length: " +
                contentLength +
                "\r\n" +
                "\r\n" +
                body;

        return response;
    }

    private String ProcessUpdateWeatherRequest(String body) {
        ObjectMapper objectMapper = AggregationServer.getObjectMapper();
        UpdateWeatherRequest rq;
        String response;

        try {
            rq = objectMapper.readValue(body, UpdateWeatherRequest.class);
            if (!historyFileHandler.IsFileExist()) {
                System.out.println("file does not exist, initializing...");
                historyFileHandler.CreateHistoryFile();
                System.out.println("file initializing finished, weather updating...");
                historyFileHandler.UpdateWeather(rq.weatherData);
                historyFileHandler.ClockIncrement(rq.lamportClock.counter);
                HistoryContent historyContent = historyFileHandler.LoadContent();
                response = CreateResponse(HttpStatus.HTTP_CREATED, objectMapper.writeValueAsString(historyContent.clock));

            } else {
                System.out.println("weather updating...");
                historyFileHandler.UpdateWeather(rq.weatherData);
                historyFileHandler.ClockIncrement(rq.lamportClock.counter);
                HistoryContent historyContent = historyFileHandler.LoadContent();
                response = CreateResponse(HttpStatus.HTTP_SUCCESS, objectMapper.writeValueAsString(historyContent.clock));
            }
        } catch (Exception e) {
            response = CreateResponse(HttpStatus.HTTP_INTERNAL_ERROR, e.getMessage());
        }

        return response;
    }

    private String GetWeatherRequest() {
        String data = historyFileHandler.GetWeather();
        return CreateResponse(HttpStatus.HTTP_SUCCESS, data);
    }
}
