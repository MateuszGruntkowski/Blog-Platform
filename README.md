# Blog Platform

A full-featured blog platform with post creation, drafts, image uploads, comments, likes, and admin moderation. Users can write and share content, while admins manage tags, categories, and moderate posts.

## Demo (YT video)

[![Watch the demo](https://img.shields.io/badge/▶_Watch_Demo-YouTube-red?style=for-the-badge&logo=youtube)](https://www.youtube.com/watch?v=X5pyNqrwhNY)

## Screenshots
![Home Page](Screenshots/Home.png)
![Post](Screenshots/Post.png)
![Comments](Screenshots/Comments.png)
![New Post](Screenshots/NewPost.png)
![Draft Posts](Screenshots/DraftPosts.png)
![Categories](Screenshots/Categories.png)

## Main Features

- **Authentication**: User registration and login (JWT)  
- **Posts**: CRUD operations, drafts, likes, image uploads  
- **Comments**: Add and delete comments  
- **Categories**: Category management (ADMIN only)  
- **Tags**: Tag management (ADMIN only)  

## Technologies

- Spring Boot 3.x  
- Spring Security + JWT  
- PostgreSQL 
- Spring Data JPA
- React 19
- OpenAPI/Swagger  

## API Documentation

Full API documentation is available in Swagger UI once the application is running:

**URL:** `http://localhost:8080/swagger-ui/index.html`

The documentation includes:  
- All endpoints
- Request/response schemas  
- Ability to test endpoints directly in the browser  
- JWT Bearer Token support  

### Authorization in Swagger

For endpoints requiring authorization:  
1. Log in using the `/api/v1/auth/login` endpoint  
2. Copy the returned token  
3. Click the **"Authorize"** button in Swagger UI  
4. Paste the token (without the `Bearer` prefix)  
5. Click **"Authorize"** 

## Quick Start (Running the project)

You can run the entire stack (Frontend, Backend, Database) with a single command thanks to Docker.

### Prerequisites
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.
* (Optional) [Git](https://git-scm.com/) to clone the repository.

### Installation & Launch

1. **Clone the repository:**
```bash
   git clone https://github.com/MateuszGruntkowski/Blog-Platform.git
   cd <project-folder-name>
```

2. **Prepare environment variables:**
   The project uses default values for development, but you can customize them by creating a `.env` file based on the example:
```bash
   cp .env.example .env
```

3. **Run the application:**
```bash
   docker-compose up --build
```

4. **Access the application:**
    - **Frontend:** http://localhost:3000
    - **Swagger UI (API Docs):** http://localhost:8080/swagger-ui/index.html
    - **Adminer (DB Management):** http://localhost:8888

## Default Credentials
- **Admin User:** `user@test.com` / `password`
- **Database:**
  - Database name: `postgres`
  - Database user: `postgres`
  - Database password: `password`


