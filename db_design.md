erDiagram
    users {
        UUID id PK
        varchar(255) username
        varchar(255) email
        varchar(255) password_hashed
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    refresh_tokens {
        uuid id PK "gen_random_uuid()"
        uuid user_id FK "REFERENCES users(id) ON DELETE CASCADE"
        varchar token_hash UK "NOT NULL (SHA-256 hash)"
        timestamptz expires_at "NOT NULL"
        timestamptz created_at "DEFAULT CURRENT_TIMESTAMP"
        timestamptz revoked_at "NULLABLE"
        varchar replaced_by_token "NULLABLE (for token rotation)"
        varchar ip_address "NULLABLE"
    }

    products {
        UUID id PK
        UUID primary_picture_id
        varchar(500) name
        varchar(1000) description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    picture {
        UUID id PK
        UUID product_variant_id FK
        int index_order
        varchar(100) name
        varchar(100) color
    }

    product_categories {
        UUID product_id FK
        UUID category_id FK
    }

    categories {
        UUID id PK
        UUID parent_id FK "Nullable (for sub-categories)"
        varchar(255) name "e.g., 'Women', 'Baby', 'Men'"
        varchar(255) description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    product_variants {
        UUID id PK
        UUID product_id FK
        int quantity
        varchar(100) color
        varchar(100) size
        double price
        timestamp updated_at
    }

    product_discount {
        UUID product_id FK
        UUID discount_id FK
    }

    discounts {
        UUID id PK
        double value
        varchar(500) description
        timestamp valid_from
        timestamp valid_until
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    user_voucher {
        UUID user_id
        UUID vouchers_id
        int usage
        int usage_limit
    }

    vouchers {
        UUID id PK
        double value
        double minimum_spend
        timestamp valid_from
        timestamp valid_until
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    tags {
        UUID id PK
        varchar(50) code
        varchar(255) name
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    product_tags {
        UUID product_id FK
        UUID tag_id FK
    }

    orders {
        UUID id PK
        UUID user_id FK
        UUID voucher_id FK
        varchar(20) phone_number
        double total_amount
        varchar(50) status
        varchar(500) address
        varchar(1000) description
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        UUID id PK
        UUID order_id FK
        UUID product_variant_id FK
        int quantity
        double unit_price
    }

    carts {
        UUID id PK
        UUID user_id FK
        timestamp created_at
        timestamp updated_at
    }

    cart_items {
        UUID id PK
        UUID cart_id FK
        UUID product_variant_id FK
        int quantity
    }

    %% Product Categories (Many-to-Many)
    products ||--o{ product_categories : "belongs_to"
    categories ||--o{ product_categories : "includes"

    %% Relationships
    users ||--o{ refresh_tokens : "generates"
    users ||--o{ orders : "places"
    users ||--|| carts : "owns"
    
    product_variants ||--o{ picture : "has_images"
    products ||--o{ product_variants : "has_variants"
    
    %% Product Discounts (Many-to-Many via junction)
    products ||--o{ product_discount : "has"
    discounts ||--o{ product_discount : "applied_to"
    
    %% Product Tags (Many-to-Many via junction)
    products ||--o{ product_tags : "categorized_by"
    tags ||--o{ product_tags : "describes"
    
    %% Orders
    orders ||--o{ order_items : "contains"
    product_variants ||--o{ order_items : "sold_as"
    vouchers |o--o{ orders : "applied_to"
    
    %% Carts
    carts ||--o{ cart_items : "holds"
    product_variants ||--o{ cart_items : "added_as"