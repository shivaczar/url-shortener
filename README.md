# URL Shortener

A simple URL shortening service that converts long URLs into short, easy-to-share links.

## Features
- Shorten long URLs
- Retrieve the original URL using the short code
- Delete a short URL when no longer needed
- View recent URLs
- View top shortened and clicked URLs
- Check API health status

## How To Run

Run the application using Maven:
```sh
mvn spring-boot:run
```

Run tests:
```sh
mvn test
```

## API Endpoints

### 1️⃣ Shorten a URL (POST)
**Endpoint:** `/api/urls/shorten`

**Request:**
```sh
curl --location 'http://localhost:8080/api/urls/shorten' \
--header 'X-API-KEY: API_KEY_12345' \
--header 'Content-Type: application/json' \
--data '{
    "url" : "https://example.com"
}'
```

**Response:**
```json
{
    "shortCode": "<SHORT_CODE_HERE>"
}
```

### 2️⃣ Retrieve the Original URL (GET)
**Endpoint:** `/api/redirect?code=<SHORT_CODE_HERE>`

**Request:**
```sh
curl --location 'http://localhost:8080/api/urls/redirect?code=wAIVtI'
```

**Response:**
- **302 Found** (Redirects to the original URL)
- **404 Not Found** (If the short code does not exist)

### 3️⃣ Delete a Short URL (DELETE)
**Endpoint:** `/api/urls/delete?code=<SHORT_CODE_HERE>`

**Request:**
```sh
curl --location --request DELETE 'http://localhost:8080/api/urls/delete/WyLDA7'
```

**Response:**
- **200 OK** (If successfully deleted)
- **404 Not Found** (If the short code does not exist)

### 4️⃣ Get Recent URLs (GET)
**Endpoint:** `/api/urls/recent`

**Request:**
```sh
curl --location 'http://localhost:8090/api/urls/recent'
```

### 5️⃣ Get All URLs (GET)
**Endpoint:** `/api/urls/all`

**Request:**
```sh
curl --location 'http://localhost:8080/api/urls/all'
```

### 6️⃣ Get Top 10 Shortened URLs (GET)
**Endpoint:** `/api/urls/top-shortened`

**Request:**
```sh
curl --location 'http://localhost:8080/api/urls/top-shortened'
```

### 7️⃣ Get Top 10 Clicked URLs (GET)
**Endpoint:** `/api/urls/top-clicked`

**Request:**
```sh
curl --location 'http://localhost:8080/api/urls/top-clicked'
```

### 8️⃣ Check API Health (GET)
**Endpoint:** `/health`

**Request:**
```sh
curl --location 'http://localhost:8091/health'
```

## Contributing
Feel free to fork the repo, submit pull requests, and suggest improvements!

## License
This project is licensed under the MIT License.
