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
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class UserRoleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserRoleServiceApplication.class, args);
	}

    @Bean
    RouterFunction<?> routes(RoleService roleService, UserService userService) {
        return nest(path("/api/user"),
                route(method(HttpMethod.GET),
                        request -> ok().body(userService.findAll(), User.class))
                        .andRoute(GET("/{id}"),
                                request -> userService.findById(request.pathVariable("id"))
                                        .flatMap(u -> ok().body(Mono.just(u), User.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                        .andRoute(method(HttpMethod.POST).and(contentType(MediaType.APPLICATION_JSON)),
                                request -> userService.addNewUser(request.bodyToMono(User.class))
                                        .flatMap(user -> ok().body(Mono.just(user), User.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                        .andRoute(PUT("/{id}").and(contentType(MediaType.APPLICATION_JSON)),
                                request -> userService.updateUser(request.bodyToMono(User.class))
                                        .flatMap(u -> ok().body(Mono.just(u), User.class))
                                        .switchIfEmpty(ServerResponse.notFound().build()))
                        .andNest(path("/api/role"),
                                route(method(HttpMethod.GET),
                                        request -> ok().body(roleService.findAll(), Role.class))
                                        .andRoute(GET("/{id}"),
                                                request -> roleService.findById(request.pathVariable("id"))
                                                        .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                        .switchIfEmpty(ServerResponse.notFound().build()))
                                        .andRoute(method(HttpMethod.POST).and(accept(MediaType.APPLICATION_JSON)),
                                                request -> roleService.addNewRole(request.bodyToMono(Role.class))
                                                        .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                        .switchIfEmpty(ServerResponse.badRequest().build()))
                                        .andRoute(DELETE("/{id}"),
                                                request -> roleService.deleteById(request.pathVariable("id"))
                                                        .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                        .switchIfEmpty(ok().build()))
                                        .andRoute(PUT("/{id}").and(contentType(MediaType.APPLICATION_JSON)),
                                                request -> roleService.updateRole(request.bodyToMono(Role.class))
                                                        .flatMap(role -> ok().body(Mono.just(role), Role.class))
                                                        .switchIfEmpty(ServerResponse.notFound().build()))));
    }

    @Bean
	CommandLineRunner demo(UserRepository userRepository, RoleRepository roleRepository) {

        return args -> {
            userRepository
                    .deleteAll()
                    .subscribe(null, null,
                            () -> Stream.of("R1,R2,R3".split(","))
                                    .map(r -> new Role(UUID.randomUUID().toString(), r, "En beskrivning av " + r))
                                    .forEach(role -> roleRepository.save(role).subscribe(System.out::println)));
            roleRepository
                    .deleteAll()
                    .subscribe(null, null,
                            () -> Stream.of("user1:Karl Benknäckare,user2:Britta Andehaag,user3:Walter Iskugel".split(","))
                                    .map(this::createUser)
                                    .forEach(user -> userRepository.save(user).subscribe(System.out::println))
                    );
        };
    }

    User createUser(String s) {
        String[] ss = s.split(":");
        return User.builder()
                .userId(ss[0])
                .name(ss[1])
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
@Builder
@Data
class User {
	@Id
	private String userId;
    private String name;
    @DBRef
	private List<Role> roles = new ArrayList<>();

    public User copy() {
        return User.builder()
                .userId(userId)
                .name(name)
                .roles(roles.stream().filter(Objects::nonNull).map(Role::copy).collect(Collectors.toList()))
                .build();
    }
}

@Document
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Data
class Role {
	@Id
	private String id;
	private String rolename;
	private String description;

    public Role copy() {
        return Role.builder()
                .description(description)
                .rolename(rolename)
                .id(id)
                .build();
    }
}