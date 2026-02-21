package com.mgrunt.blog.config;

import com.mgrunt.blog.domain.PostStatus;
import com.mgrunt.blog.domain.entities.Category;
import com.mgrunt.blog.domain.entities.Post;
import com.mgrunt.blog.domain.entities.Tag;
import com.mgrunt.blog.domain.entities.User;
import com.mgrunt.blog.repositories.CategoryRepository;
import com.mgrunt.blog.repositories.PostRepository;
import com.mgrunt.blog.repositories.TagRepository;
import com.mgrunt.blog.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Override
    public void run(String... args) throws Exception {
        if (postRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        System.out.println("Initializing startup data...");

        String email = "user@test.com";
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Startup user not found. Check SecurityConfig."));

        Category category = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name("Technology")
                                .build()
                ));

        Tag tagJava = tagRepository.findByName("Java")
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .name("Java")
                                .build()
                ));

        Tag tagSpring = tagRepository.findByName("Spring Boot")
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .name("Spring Boot")
                                .build()
                ));

        Set<Tag> tags = new HashSet<>();
        tags.add(tagJava);
        tags.add(tagSpring);

        // 4. Stwórz Posta
        Post post = Post.builder()
                .title("Welcome to Blog Platform!")
                .content("This is a sample post generated automatically to showcase the application's capabilities.\n\n" +
                        "Feel free to explore the core features implemented in this project:\n" +
                        "- Creating new Categories and Tags\n" +
                        "- Publishing Posts and managing them as an Admin (including Deleting)\n" +
                        "- Creating and editing Drafts\n" +
                        "- Interaction: Adding and Deleting comments (Admin has full moderation rights)\n" +
                        "This data was seeded automatically on the first Docker launch to provide a complete demo experience.")
                .status(PostStatus.PUBLISHED)
                .readingTime(5)
                .author(author)
                .category(category)
                .tags(tags)
                .comments(Collections.emptyList())
                .likes(new HashSet<>())
                .build();

        postRepository.save(post);

        System.out.println("Starter post created: " + post.getTitle());
    }
}
