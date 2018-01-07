package com.example.userroleservice;

import lombok.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class UserRoleServiceApplication {

    public static final String LOCATION_API_USER = "http://localhost:8080/api/user";
    public static final String LOCATION_API_ROLE = "http://localhost:8080/api/role";

    public static void main(String[] args) {
		SpringApplication.run(UserRoleServiceApplication.class, args);
	}

    @Bean
    RouterFunction<?> routes(RoleService roleService, UserService userService) {
        return nest(path("/api/user"),
                route(GET("/{id}"),
                        request -> userService.findById(request.pathVariable("id"))
                                .flatMap(u -> ok().body(Mono.just(u), User.class))
                                .switchIfEmpty(ServerResponse.notFound().build()))
                        .andRoute(method(HttpMethod.GET),
                                request -> ok().body(userService.findAll(), User.class))
                        .andRoute(method(HttpMethod.POST).and(contentType(MediaType.APPLICATION_JSON)),
                                request -> userService.addNewUser(request.bodyToMono(User.class))
                                        .flatMap(user -> created(location(user)).body(Mono.just(user), User.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                        .andRoute(PUT("/{id}").and(contentType(MediaType.APPLICATION_JSON)),
                                request -> userService.updateUser(request.bodyToMono(User.class))
                                        .flatMap(u -> ok().body(Mono.just(u), User.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                        .andRoute(DELETE("/{id}"),
                                request -> userService.deleteById(request.pathVariable("id"))
                                .flatMap(user -> ok().body(Mono.just(user), User.class))
                                .switchIfEmpty(ok().build())
                        ))
                .andNest(path("/api/role"),
                        route(GET("/{id}"),
                                request -> roleService.findById(request.pathVariable("id"))
                                        .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                                .andRoute(method(HttpMethod.GET),
                                        request -> ok().body(roleService.findAll(), Role.class))
                                .andRoute(method(HttpMethod.POST).and(accept(MediaType.APPLICATION_JSON)),
                                        request -> roleService.addNewRole(request.bodyToMono(Role.class))
                                                .flatMap(role -> created(location(role)).body(Mono.just(role), Role.class))
                                                .switchIfEmpty(ServerResponse.badRequest().build()))
                                .andRoute(DELETE("/{id}"),
                                        request -> roleService.deleteById(request.pathVariable("id"))
                                                .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                .switchIfEmpty(ok().build()))
                                .andRoute(PUT("/{id}").and(contentType(MediaType.APPLICATION_JSON)),
                                        request -> roleService.updateRole(request.bodyToMono(Role.class))
                                                .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                .switchIfEmpty(ServerResponse.notFound().build())));
    }

    private URI location(User user) {
        try {
            return new URI(format("%s/%s", LOCATION_API_USER, user.getUserId()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Can't create location for user:" + user, e);
        }
    }
    private URI location(Role role) {
        try {
            return new URI(format("%s/%s", LOCATION_API_ROLE, role.getId()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Can't create location for role:" + role, e);
        }
    }
    @Bean
    CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        PathPatternParser patternParser = new PathPatternParser();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(patternParser);
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
    @Bean
	CommandLineRunner demo(UserRepository userRepository, RoleRepository roleRepository) {

        return args -> {
            userRepository
                    .deleteAll()
                    .subscribe(null, null,
                            () -> Stream.of("R1:1-1-1-1-1,R2:1-1-1-1-2,R3:1-1-1-1-3".split(","))
                                    .map(this::createRole)
                                    .forEach(role -> roleRepository.save(role).subscribe(System.out::println)));
            roleRepository
                    .deleteAll()
                    .subscribe(null, null,
                            () -> Stream.of("user1:Karl Benknäckare:1-1-1-1-1,user2:Britta Andehaag,user3:Walter Iskugel".split(","))
                                    .map(this::createUser)
                                    .forEach(user -> userRepository.save(user).subscribe(System.out::println))
                    );
        };
    }

    User createUser(String s) {
        String[] ss = s.split(":");

        ArrayList<Role> roles = new ArrayList<>();
        if (ss.length > 2) {
            roles.add(Role.builder().id(ss[2]).build());
        }

        return User.builder()
                .userId(ss[0])
                .name(ss[1])
                .roles(roles)
                .build();
    }

    Role createRole(String s) {
        String[] ss = s.split(":");
        return Role.builder()
                .rolename(ss[0])
                .id(ss[1])
                .description("Beskrivning av roll " + ss[0])
                .build();
    }

}

@Service
class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Flux<User> findAll() {
        return repository.findAll().map(User::copy);
    }

    public  Mono<User> findById(String id) {
        return repository.findById(id).map(User::copy);
    }

    public Mono<User> deleteById(String id) {
        return repository.findById(id)
                .flatMap(oldUser -> repository
                        .deleteById(id)
                        .then(Mono.just(oldUser).map(User::copy)));
    }

    public Mono<User> updateUser(Mono<User> user) {
        return user.flatMap(u -> repository
                .findById(u.getUserId())
                .flatMap(u2 -> repository.save(u)))
                .map(User::copy);
    }

    public Mono<User> addNewUser(Mono<User> user) {
        return repository.saveAll(user)
                .map(User::copy)
                .singleOrEmpty();
    }
}

@Service
class RoleService {

    private final RoleRepository repository;
    public RoleService(RoleRepository repository) {
        this.repository = repository;
    }

    public Flux<Role> findAll() {
        return repository.findAll().map(Role::copy);
    }

    public  Mono<Role> findById(String id) {
        return repository.findById(id).map(Role::copy);
    }

    public Mono<Role> deleteById(String id) {
        return repository.findById(id)
                .flatMap(oldRole ->
                        repository.deleteById(id)
                                .then(Mono.just(oldRole)));
    }

    public Mono<Role> updateRole(Mono<Role> role) {
        return role.flatMap(r ->
                repository.findById(r.getId())
                        .flatMap(r2 -> repository.save(r))
        );
    }

    public Mono<Role> addNewRole(Mono<Role> role) {
        return repository.saveAll(role.map(this::withRandomUuid))
                .singleOrEmpty();
    }

    private  Role withRandomUuid(Role role) {
        return Role.builder()
                .id(UUID.randomUUID().toString())
                .rolename(role.getRolename())
                .description(role.getDescription())
                .build();
    }
}

interface UserRepository extends ReactiveMongoRepository<User, String>{}
interface RoleRepository extends ReactiveMongoRepository<Role, String>{}

@Document
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder(toBuilder = true)
@Data
class User {
	@Id
	private String userId;
    private String name;
    @DBRef
	@Builder.Default
    private List<Role> roles = new ArrayList<>();

    public User copy() {
        return this.toBuilder()
                .roles(roles.stream().filter(Objects::nonNull).map(Role::copy).collect(Collectors.toList()))
                .build();
    }
}

@Document
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder(toBuilder = true)
@Data
class Role {
	@Id
	private String id;
	private String rolename;
	private String description;

    public Role copy() {
        return this.toBuilder().build();
    }
}