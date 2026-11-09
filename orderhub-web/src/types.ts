/*
  TYPES
  These TypeScript types describe the JSON your Spring Boot API sends and receives.
  They mirror the Java records (DTOs) in the backend. If you rename a field in a DTO,
  rename it here too and the compiler will point at every place that needs updating.
*/

/** What GET list endpoints with Pageable return (the backend's PageResponse record). */
export interface PageResponse<T> {
  content: T[];
  page: number; // zero-based, just like Spring
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---------- auth ----------
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}
export interface RegisterResponse {
  id: number;
  email: string;
  role: string;
}

// ---------- catalog ----------
export interface Category {
  id: number;
  name: string;
  parentId: number | null;
}
export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number; // BigDecimal arrives as a JSON number
  categoryId: number;
  categoryName: string;
  active: boolean;
}
export interface ProductInput {
  name: string;
  description: string;
  price: number;
  categoryId: number;
}
export interface StockResponse {
  productId: number;
  availableQty: number;
  reservedQty: number;
}

// ---------- cart ----------
export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}
export interface Cart {
  cartId: number;
  items: CartItem[];
  total: number;
}

// ---------- addresses ----------
export interface Address {
  id: number;
  label: string | null;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  defaultAddress: boolean;
  createdAt: string;
}
/** The body for POST/PUT /addresses (the backend's AddressRequest). */
export interface AddressInput {
  label: string;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  makeDefault: boolean;
}

// ---------- orders ----------
export type OrderStatus = "CREATED" | "PAID" | "SHIPPED" | "DELIVERED" | "CANCELLED";

export interface ShippingAddress {
  name: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}
export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number; // the price at the moment of purchase
  lineTotal: number;
}
/** Full order, returned by checkout and by the detail endpoints. */
export interface Order {
  id: number;
  status: OrderStatus;
  totalAmount: number;
  expiresAt: string | null;
  createdAt: string;
  items: OrderItem[];
  shippingAddress: ShippingAddress | null;
}
/** Short order, returned in lists. */
export interface OrderSummary {
  id: number;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  expiresAt?: string | null;
}

// ---------- payments ----------
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "REFUND_PENDING" | "REFUNDED";
export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  status: PaymentStatus;
  providerRef: string | null;
  createdAt: string;
}
