# URL Shortener

A simple URL shortening service that converts long URLs into short, easy-to-share links.

## Features
- Shorten long URLs
- Retrieve the original URL using the short code
- Delete a short URL when no longer needed

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
**Endpoint:** `/api/shorten`

**Request:**
```sh
curl --location 'http://localhost:8090/api/shorten' \
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
curl --location 'http://localhost:8090/api/redirect?code=wAIVtI'
```

**Response:**
- **302 Found** (Redirects to the original URL)
- **404 Not Found** (If the short code does not exist)

### 3️⃣ Delete a Short URL (DELETE)
**Endpoint:** `/api/delete?code=<SHORT_CODE_HERE>`

**Request:**
```sh
curl --location --request DELETE 'http://localhost:8090/api/delete/WyLDA7'
```

**Response:**
- **200 OK** (If successfully deleted)
- **404 Not Found** (If the short code does not exist)

## Contributing
Feel free to fork the repo, submit pull requests, and suggest improvements!

## License
This project is licensed under the MIT License.

