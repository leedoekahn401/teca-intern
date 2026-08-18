erDiagram
    users {
        UUID id PK
        varchar(255) username UK
        varchar(255) email UK
        varchar(255) password_hashed
        varchar(50) role "e.g. 'ADMIN', 'CUSTOMER', 'STAFF'"
        varchar(50) status "e.g. 'ACTIVE', 'INACTIVE', 'BANNED'"
        boolean is_email_verified "DEFAULT false"
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
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
        varchar(500) name
        varchar(1000) description
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    product_images {
        UUID id PK
        UUID product_id FK
        UUID product_variant_id FK "Nullable (for variant-specific images)"
        varchar(500) image_url
        varchar(100) name "Nullable"
        varchar(100) color "Nullable"
        int index_order
        boolean is_primary "True for main product picture"
        timestamptz created_at
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
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    product_variants {
        UUID id PK
        UUID product_id FK
        varchar(100) sku UK "Stock Keeping Unit e.g., '5S-POLO-BLK-L'"
        int quantity
        varchar(100) color
        varchar(100) size
        decimal(15,2) price
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    product_discount {
        UUID product_id FK
        UUID discount_id FK
    }

    discounts {
        UUID id PK
        varchar(50) discount_type "'PERCENTAGE' or 'FIXED_AMOUNT'"
        decimal(15,2) value
        decimal(15,2) max_discount_amount "Nullable (Cap for percentage discounts)"
        varchar(500) description
        timestamptz valid_from
        timestamptz valid_until
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    user_voucher {
        UUID user_id FK
        UUID voucher_id FK
        int usage
        int usage_limit
    }

    vouchers {
        UUID id PK
        varchar(50) code UK "e.g., 'SUMMER2026', 'WELCOME50'"
        varchar(50) discount_type "'PERCENTAGE' or 'FIXED_AMOUNT'"
        decimal(15,2) value
        decimal(15,2) max_discount_amount "Nullable (Cap for percentage discounts)"
        decimal(15,2) minimum_spend
        timestamptz valid_from
        timestamptz valid_until
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    tags {
        UUID id PK
        varchar(50) code
        varchar(255) name
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    product_tags {
        UUID product_id FK
        UUID tag_id FK
    }

    orders {
        UUID id PK
        UUID user_id FK
        UUID voucher_id FK
        varchar(255) recipient_name
        varchar(20) phone_number
        decimal(15,2) subtotal_amount "Tổng tiền hàng"
        decimal(15,2) shipping_fee "Phí vận chuyển"
        decimal(15,2) discount_amount "Tiền giảm giá voucher/khuyến mãi"
        decimal(15,2) final_amount "Tổng thanh toán thực tế"
        varchar(50) status
        varchar(500) address
        varchar(1000) description
        timestamptz created_at
        timestamptz updated_at
    }

    order_items {
        UUID id PK
        UUID order_id FK
        UUID product_variant_id FK "Nullable (ON DELETE SET NULL)"
        varchar(255) product_name "Snapshot: product name at checkout"
        varchar(100) sku "Snapshot: SKU at checkout"
        varchar(100) color "Snapshot: color at checkout"
        varchar(100) size "Snapshot: size at checkout"
        varchar(500) thumbnail_url "Snapshot: image URL at checkout"
        int quantity
        decimal(15,2) unit_price "Snapshot: unit price at checkout"
        decimal(15,2) total_price "quantity * unit_price"
    }

    carts {
        UUID id PK
        UUID user_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    cart_items {
        UUID id PK
        UUID cart_id FK "Composite UK (cart_id, product_variant_id)"
        UUID product_variant_id FK "Composite UK (cart_id, product_variant_id)"
        int quantity
        timestamptz created_at
        timestamptz updated_at
    }

    %% Product Categories (Many-to-Many)
    products ||--o{ product_categories : "belongs_to"
    categories ||--o{ product_categories : "includes"

    %% Relationships
    users ||--o{ refresh_tokens : "generates"
    users ||--o{ orders : "places"
    users ||--|| carts : "owns"
    
    products ||--o{ product_images : "has_images"
    product_variants |o--o{ product_images : "variant_images"
    products ||--o{ product_variants : "has_variants"
    
    %% Product Discounts (Many-to-Many via junction)
    products ||--o{ product_discount : "has"
    discounts ||--o{ product_discount : "applied_to"

    %% User Vouchers (Many-to-Many via junction)
    users ||--o{ user_voucher : "claims"
    vouchers ||--o{ user_voucher : "assigned_to"
    
    %% Product Tags (Many-to-Many via junction)
    products ||--o{ product_tags : "categorized_by"
    tags ||--o{ product_tags : "describes"
    
    %% Orders
    orders ||--o{ order_items : "contains"
    product_variants |o--o{ order_items : "sold_as"
    vouchers |o--o{ orders : "applied_to"
    
    %% Carts
    carts ||--o{ cart_items : "holds"
    product_variants ||--o{ cart_items : "added_as"