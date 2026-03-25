# Community Discussion & Moderation Platform 🚀

## 📌 Overview

A backend system built using Spring Boot that allows users to create communities, post content, interact through comments and votes, and enables moderators to manage reports and maintain platform quality.

---

## ⚙️ Tech Stack

* Java 17
* Spring Boot
* Spring Security + JWT Authentication
* Spring Data JPA (Hibernate)
* MySQL
* Maven

---

## 🔑 Key Features

### 👤 Authentication

* User registration and login
* JWT-based authentication and authorization

### 🏘️ Communities

* Create and manage communities (Moderator only)
* View all communities and details

### 📝 Posts

* Create, view, and delete posts
* Pagination support
* Trending posts based on activity

### 💬 Comments

* Add and view comments on posts
* Delete own comments

### 👍 Voting System

* Upvote / Downvote posts
* Toggle voting logic

### ⭐ Reviews

* Create and view reviews for subjects
* Rating-based system

### 🚨 Reporting & Moderation

* Report posts, comments, or reviews
* Moderators can:

    * Resolve reports (content removed)
    * Dismiss reports
* Status tracking (PENDING, RESOLVED, DISMISSED)

---

## 🔐 Security

* Role-based access control (USER, MODERATOR)
* JWT token validation using filters
* Protected API endpoints

---

## 🧪 API Testing

Tested using Postman:

* Authentication
* Community creation
* Post lifecycle
* Comments, votes, reviews
* Report & moderation flow

---

## 📊 Highlights

* Designed scalable REST APIs with layered architecture
* Implemented soft delete for audit and data integrity
* Enforced database constraints (e.g., one vote per user per post)
* Optimized reads using denormalized counters (upvotes, comments)

---

## 🚀 How to Run

1. Clone the repository
2. Create MySQL database:

   ```sql
   CREATE DATABASE community_db;
   ```
3. Update `application.properties` with your DB credentials
4. Run the Spring Boot application

---

## 📌 Future Improvements

* Frontend integration (React)
* Advanced moderation dashboard
* Caching (Redis)
* API documentation (Swagger)

---

## 👨‍💻 Author

Sujal Dhawan
