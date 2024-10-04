import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestHandlerTest {

    private RequestHandler requestHandler;
    private HistoryFileHandler mockHistoryFileHandler;

    @BeforeEach
    void setUp() {
        mockHistoryFileHandler = mock(HistoryFileHandler.class);
        requestHandler = new RequestHandler(mockHistoryFileHandler);
    }

    @Test
    void testHandleRequest_PutRequest_FileNotExist() throws Exception {
        String request = "PUT /weather\n\n{\"weatherData\":{\"id\":\"1\"},\"lamportClock\":{\"counter\":1}}";
        when(mockHistoryFileHandler.IsFileExist()).thenReturn(false);
        doNothing().when(mockHistoryFileHandler).CreateHistoryFile();
        doNothing().when(mockHistoryFileHandler).UpdateWeather(any(WeatherData.class));
        when(mockHistoryFileHandler.LoadContent()).thenReturn(new HistoryContent(new ArrayList<>(), new LamportClock()));

        String actual = requestHandler.HandleRequest(request);

        assertTrue(actual.contains("201 HTTP_CREATED"));
        verify(mockHistoryFileHandler).CreateHistoryFile();
        verify(mockHistoryFileHandler).UpdateWeather(any(WeatherData.class));
        verify(mockHistoryFileHandler).LoadContent();
    }

    @Test
    void testHandleRequest_PutRequest_FileExists() throws Exception {

        String request = "PUT /weather\n\n{\"weatherData\":{\"id\":\"1\"},\"lamportClock\":{\"counter\":2}}";
        when(mockHistoryFileHandler.IsFileExist()).thenReturn(true);
        doNothing().when(mockHistoryFileHandler).UpdateWeather(any(WeatherData.class));
        LamportClock lamportClock = new LamportClock();
        lamportClock.counter = 2;
        when(mockHistoryFileHandler.LoadContent()).thenReturn(new HistoryContent(new ArrayList<>(), lamportClock));

        String actual = requestHandler.HandleRequest(request);

        assertTrue(actual.contains("200 HTTP_SUCCESS"));
        assertTrue(actual.contains("\"counter\":2"));
        verify(mockHistoryFileHandler).UpdateWeather(any(WeatherData.class));
        verify(mockHistoryFileHandler).LoadContent();
    }

    @Test
    void testHandleRequest_GetRequest() throws Exception {
        String request = "GET all";
//        when(mockHistoryFileHandler.LoadContent()).thenReturn(new HistoryContent(new ArrayList<>(), new LamportClock()));
        when(mockHistoryFileHandler.GetWeather(any())).thenReturn("\"weatherData\":[],\"counter\":0");
        String actual = requestHandler.HandleRequest(request);

        assertTrue(actual.contains("200 HTTP_SUCCESS"));
        assertTrue(actual.contains("\"weatherData\""));
        assertTrue(actual.contains("\"counter\""));
        verify(mockHistoryFileHandler).GetWeather(any());
    }

    @Test
    void testHandleRequest_BadRequest() {
        String request = "POST /unknown\n";
        String response = requestHandler.HandleRequest(request);

        assertTrue(response.contains("400 HTTP_BAD_REQUEST"));
    }

    @Test
    void testProcessUpdateWeatherRequest_ExceptionHandling() throws Exception {
        String request = "PUT /weather\n\n{\"invalid\":}";
        when(mockHistoryFileHandler.IsFileExist()).thenThrow(new RuntimeException("Mock Exception"));

        String actual = requestHandler.HandleRequest(request);

        assertTrue(actual.contains("500 HTTP_INTERNAL_ERROR"));
    }
}