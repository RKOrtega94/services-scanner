package com.rkortega94.scanner;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.rkortega94.scanner.dtos.MethodDataDTO;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.dtos.ScannedServiceDTO;
import com.rkortega94.scanner.enums.ControllerTypeEnum;
import org.springframework.stereotype.Service;

class ScannerServiceTest {

    @RequestMapping("/api/v1/users")
    private static class SampleController {

        @GetMapping("/all")
        public void getAll() {
        }

        @RequestMapping(value = "/sync", method = {RequestMethod.POST, RequestMethod.PUT})
        public void sync() {
        }

        @PostMapping
        public void create() {
        }
    }

    @RestController
    @RequestMapping("/api/v1/rest")
    private static class SampleRestController {

        @GetMapping("/items")
        public void items() {
        }
    }

    @Controller
    @RequestMapping("/api/v1/mvc")
    private static class SampleMvcController {

        @GetMapping("/home")
        public String home() {
            return "home";
        }
    }

    @Service
    private static class SampleService {
        public void doSomething() {
        }
    }

    @Test
    void buildMethodDataDTO_shouldResolveShortcutMappingAndNormalizePath() throws Exception {
        ScannerService scannerService = new ScannerService(null);
        Method method = SampleController.class.getDeclaredMethod("getAll");

        MethodDataDTO dto = invokeBuildMethodDataDTO(scannerService, method, List.of("/api/v1/users/"));

        assertNotNull(dto);
        assertEquals(Set.of(RequestMethod.GET), dto.methods());
        assertEquals(Set.of("/api/v1/users/all"), dto.paths());
    }

    @Test
    void buildMethodDataDTO_shouldKeepAllRequestMethods() throws Exception {
        ScannerService scannerService = new ScannerService(null);
        Method method = SampleController.class.getDeclaredMethod("sync");

        MethodDataDTO dto = invokeBuildMethodDataDTO(scannerService, method, List.of("/api/v1/users"));

        assertNotNull(dto);
        assertEquals(Set.of(RequestMethod.POST, RequestMethod.PUT), dto.methods());
        assertEquals(Set.of("/api/v1/users/sync"), dto.paths());
    }

    @Test
    void buildMethodDataDTO_shouldUseClassPathWhenMethodPathIsEmpty() throws Exception {
        ScannerService scannerService = new ScannerService(null);
        Method method = SampleController.class.getDeclaredMethod("create");

        MethodDataDTO dto = invokeBuildMethodDataDTO(scannerService, method, List.of("/api/v1/users"));

        assertNotNull(dto);
        assertEquals(Set.of(RequestMethod.POST), dto.methods());
        assertEquals(Set.of("/api/v1/users"), dto.paths());
    }

    @Test
    void scanAll_shouldReturnServiceNameAndMvcAndRestControllers() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("spring.application.name", "scanner-test")));
        context.registerBean(SampleRestController.class);
        context.registerBean(SampleMvcController.class);
        context.refresh();

        try {
            ScannerService scannerService = new ScannerService(context);
            ScannedApplicationDTO dto = scannerService.scanAll(false);

            assertEquals("scanner-test", dto.serviceName());
            assertEquals(2, dto.controllers().size());
            assertTrue(dto.controllers().stream()
                    .anyMatch(controller -> controller.controllerName().equals("SampleRestController")
                            && controller.controllerType() == ControllerTypeEnum.REST
                            && !controller.methods().isEmpty()));
            assertTrue(dto.controllers().stream()
                    .anyMatch(controller -> controller.controllerName().equals("SampleMvcController")
                            && controller.controllerType() == ControllerTypeEnum.CONTROLLER
                            && !controller.methods().isEmpty()));
        } finally {
            context.close();
        }
    }

    @Test
    void scanAll_shouldFallbackServiceNameWhenPropertyIsMissing() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SampleRestController.class);
        context.refresh();

        try {
            ScannerService scannerService = new ScannerService(context);
            ScannedApplicationDTO dto = scannerService.scanAll(false);

            assertEquals("unknown-service", dto.serviceName());
        } finally {
            context.close();
        }
    }

    @Test
    void scanAll_shouldIncludeServices() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SampleService.class);
        context.refresh();

        try {
            ScannerService scannerService = new ScannerService(context);
            ScannedApplicationDTO dto = scannerService.scanAll(false);

            assertEquals(1, dto.services().size());
            assertTrue(dto.services().stream()
                    .anyMatch(service -> service.serviceName().equals("SampleService")
                            && service.methods().stream().anyMatch(m -> m.name().equals("doSomething"))));
        } finally {
            context.close();
        }
    }

    @SuppressWarnings("unchecked")
    private MethodDataDTO invokeBuildMethodDataDTO(ScannerService scannerService, Method method, List<String> classPaths)
            throws Exception {
        Method target = ScannerService.class.getDeclaredMethod("buildMethodDataDTO", Method.class, List.class);
        target.setAccessible(true);
        return (MethodDataDTO) target.invoke(scannerService, method, classPaths);
    }
}
