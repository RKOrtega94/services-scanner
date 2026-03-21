package com.rkortega94.scanner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.rkortega94.scanner.dtos.MethodDataDTO;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.dtos.ScannedControllerDTO;
import com.rkortega94.scanner.enums.ControllerTypeEnum;
import static com.rkortega94.scanner.utils.ControllerUtils.extractAuthorities;
import static com.rkortega94.scanner.utils.ControllerUtils.extractRoles;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScannerService {
    private static final Log log = LogFactory.getLog(ScannerService.class);
    private static final Set<RequestMethod> ALL_REQUEST_METHODS = EnumSet.allOf(RequestMethod.class);
    private static final String UNKNOWN_SERVICE = "unknown-service";
    private final ApplicationContext applicationContext;

    public ScannedApplicationDTO scanAll(Boolean includeSwagger) {
        Set<ScannedControllerDTO> controllers = new HashSet<>();
        controllers.addAll(scanControllerData(includeSwagger));
        controllers.addAll(scanRestControllerData(includeSwagger));
        scanServices();
        return buildScannedApplicationDTO(controllers);
    }

    public ScannedApplicationDTO scanControllers() {
        Set<ScannedControllerDTO> controllers = scanControllerData(false);
        return buildScannedApplicationDTO(controllers);
    }

    public ScannedApplicationDTO scanRestControllers(Boolean includeSwagger) {
        Set<ScannedControllerDTO> controllers = scanRestControllerData(includeSwagger);
        return buildScannedApplicationDTO(controllers);
    }

    public void scanServices() {
    }

    private Set<ScannedControllerDTO> scanControllerData(Boolean includeSwagger) {
        List<Class<?>> controllerClasses = applicationContext.getBeansWithAnnotation(Controller.class).values()
                .stream()
                .<Class<?>>map(AopUtils::getTargetClass)
                .filter(clazz -> !clazz.isAnnotationPresent(RestController.class))
                .filter(clazz -> includeSwagger || !isSwaggerController(clazz))
                .toList();
        log.debug("Found " + controllerClasses.size() + " MVC controllers:");

        return controllerClasses.stream()
                .map(clazz -> buildScannedControllerDTO(clazz, ControllerTypeEnum.CONTROLLER))
                .collect(Collectors.toSet());
    }

    private Set<ScannedControllerDTO> scanRestControllerData(Boolean includeSwagger) {
        List<Class<?>> controllerClasses = applicationContext.getBeansWithAnnotation(RestController.class).values()
                .stream()
                .<Class<?>>map(AopUtils::getTargetClass)
                .filter(clazz -> includeSwagger || !isSwaggerController(clazz))
                .toList();
        log.debug("Found " + controllerClasses.size() + " REST controllers:");

        return controllerClasses.stream()
                .map(clazz -> buildScannedControllerDTO(clazz, ControllerTypeEnum.REST))
                .collect(Collectors.toSet());
    }

    private ScannedControllerDTO buildScannedControllerDTO(Class<?> clazz, ControllerTypeEnum controllerType) {
        List<String> classPaths = resolveClassPaths(clazz);
        Set<String> normalizedControllerPaths = classPaths.stream()
                .map(path -> joinPaths(path, ""))
                .collect(Collectors.toSet());
        log.debug("Found paths: " + classPaths);

        Set<MethodDataDTO> methods = extractMethods(clazz, classPaths);
        log.debug("Found " + methods.size() + " methods:");

        return new ScannedControllerDTO(clazz.getSimpleName(), normalizedControllerPaths, methods, controllerType);
    }

    private ScannedApplicationDTO buildScannedApplicationDTO(Set<ScannedControllerDTO> controllers) {
        String serviceName = applicationContext != null
                ? applicationContext.getEnvironment().getProperty("spring.application.name", UNKNOWN_SERVICE)
                : UNKNOWN_SERVICE;
        return new ScannedApplicationDTO(serviceName, controllers);
    }

    private boolean isSwaggerController(Class<?> clazz) {
        return clazz.getPackageName().startsWith("springfox") || clazz.getPackageName().startsWith("org.springdoc");
    }

    private Set<MethodDataDTO> extractMethods(Class<?> clazz, List<String> classPaths) {
        Set<MethodDataDTO> methods = Arrays.stream(clazz.getDeclaredMethods()) //
                .filter(this::hasMappingAnnotation) //
                .map(method -> buildMethodDataDTO(method, classPaths)) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toSet());
        return methods;
    }

    private MethodDataDTO buildMethodDataDTO(Method method, List<String> classPaths) {
        MappingData mappingData = resolveMethodMapping(method);
        if (mappingData == null) {
            return null;
        }

        Set<String> fullPaths = buildFullPaths(classPaths, mappingData.rawPaths());
        return MethodDataDTO.builder()
                .name(method.getName())
                .methods(mappingData.methods())
                .paths(fullPaths)
                .roles(extractRoles(method))
                .authorities(extractAuthorities(method))
                .build();
    }

    private boolean hasMappingAnnotation(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private List<String> resolveClassPaths(Class<?> clazz) {
        List<String> classPaths = Arrays.stream(clazz.getAnnotationsByType(RequestMapping.class))
                .flatMap(annotation -> Arrays.stream(resolveRawPaths(annotation.value(), annotation.path())))
                .toList();

        return classPaths.isEmpty() ? List.of("") : classPaths;
    }

    private MappingData resolveMethodMapping(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);

        if (requestMapping != null) {
            RequestMethod[] declaredMethods = requestMapping.method();
            Set<RequestMethod> requestMethods = declaredMethods.length == 0
                    ? ALL_REQUEST_METHODS
                    : EnumSet.copyOf(Arrays.asList(declaredMethods));
            return new MappingData(requestMethods, resolveRawPaths(requestMapping.value(), requestMapping.path()));
        } else if (getMapping != null) {
            return new MappingData(Set.of(RequestMethod.GET), resolveRawPaths(getMapping.value(), getMapping.path()));
        } else if (postMapping != null) {
            return new MappingData(Set.of(RequestMethod.POST), resolveRawPaths(postMapping.value(), postMapping.path()));
        } else if (putMapping != null) {
            return new MappingData(Set.of(RequestMethod.PUT), resolveRawPaths(putMapping.value(), putMapping.path()));
        } else if (patchMapping != null) {
            return new MappingData(Set.of(RequestMethod.PATCH), resolveRawPaths(patchMapping.value(), patchMapping.path()));
        } else if (deleteMapping != null) {
            return new MappingData(Set.of(RequestMethod.DELETE), resolveRawPaths(deleteMapping.value(), deleteMapping.path()));
        }

        return null;
    }

    private Set<String> buildFullPaths(List<String> classPaths, String[] methodPaths) {
        List<String> basePaths = classPaths.isEmpty() ? List.of("") : classPaths;
        String[] endpointPaths = methodPaths.length == 0 ? new String[]{""} : methodPaths;

        Set<String> fullPaths = new HashSet<>();
        for (String classPath : basePaths) {
            for (String methodPath : endpointPaths) {
                fullPaths.add(joinPaths(classPath, methodPath));
            }
        }

        return fullPaths;
    }

    private String joinPaths(String basePath, String methodPath) {
        String normalizedBase = normalizePathSegment(basePath);
        String normalizedMethod = normalizePathSegment(methodPath);
        if (normalizedBase.isEmpty() && normalizedMethod.isEmpty()) {
            return "/";
        }
        if (normalizedBase.isEmpty()) {
            return ensureStartsWithSlash(normalizedMethod);
        }
        if (normalizedMethod.isEmpty()) {
            return ensureStartsWithSlash(normalizedBase);
        }

        String baseWithoutTrailing = normalizedBase.replaceAll("/+$", "");
        String methodWithoutLeading = normalizedMethod.replaceAll("^/+", "");
        return ensureStartsWithSlash(baseWithoutTrailing + "/" + methodWithoutLeading);
    }

    private String[] resolveRawPaths(String[] value, String[] path) {
        if (value.length > 0) {
            return value;
        }
        if (path.length > 0) {
            return path;
        }
        return new String[]{""};
    }

    private String normalizePathSegment(String path) {
        return path == null ? "" : path.trim();
    }

    private String ensureStartsWithSlash(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private record MappingData(Set<RequestMethod> methods, String[] rawPaths) {
    }
}
