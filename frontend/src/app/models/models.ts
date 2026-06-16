export interface Tender {
  id: number;
  tenderNumber: string;
  title: string;
  url: string;
  organizer: string;
  publishDate: string;
  deadlineDate: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
}

export interface FoundModel {
  id: number;
  productName: string;
  productUrl: string;
  price: number;
  priceByn: number;
  exchangeRate: number;
  supplierSite: string;
  matchScore: number;
  semanticScore: number;
  parametricScore: number;
  rankPosition: number;
  emails?: FoundModelEmail[];
  showEmails?: boolean;
  supplierEmail?: string | null;
  emailSubject?: string;
  emailBody?: string;
  loadingEmails?: boolean;
}

export interface FoundModelEmail {
  id: number;
  foundModelId: number;
  direction: 'OUT' | 'IN';
  subject: string;
  body: string;
  fromEmail: string;
  toEmail: string;
  status: string;
  errorMessage: string | null;
  messageId: string | null;
  createdAt: string;
}

export interface TenderItem {
  id: number;
  lotNumber: string;
  description: string;
  originalDescription: string;
  quantity: number;
  unit: string;
  estimatedPrice: number;
  currency: string;
  okpd2Code: string;
  foundModelName: string;
  foundModelUrl: string;
  foundModelPrice: number;
  foundModelPriceByn: number;
  foundModelExchangeRate: number;
  deliveryCostByn: number;
  markupPercent: number;
  finalPriceByn: number;
  supplierSite: string;
  status: string;
  documentDescription: string;
  documentUrl: string;
  documentFileName: string;
  extractedParams: string | null;
  matchScore: number | null;
  foundModels: FoundModel[];
  selectedFoundModelId?: number | null;
  showEmails?: boolean;
}

export interface LogEntry {
  id: number;
  tenderId: number;
  tenderNumber: string;
  step: string;
  message: string;
  level: 'INFO' | 'DEBUG' | 'WARNING' | 'ERROR' | 'SUCCESS';
  details: string;
  createdAt: string;
}

export interface DashboardStats {
  totalTenders: number;
  newTenders: number;
  processingTenders: number;
  completedTenders: number;
  errorTenders: number;
  totalEmailsSent: number;
  pendingEmails: number;
  totalItems: number;
  itemsFound: number;
  itemsNotFound: number;
}

export interface Supplier {
  id: number;
  name: string;
  siteUrl: string;
  email: string;
  phone: string;
  contactPerson: string;
  isActive: boolean;
}

export interface TelegramMessage {
  id: number;
  messageId: number;
  channelId: number;
  text: string;
  sender: string;
  hasLink: boolean;
  extractedUrl: string;
  status: string;
  createdAt: string;
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'USER';
  status: 'ACTIVE' | 'BLOCKED';
}

export interface Config {
  id: number;
  key: string;
  value: string;
  description: string;
  group: 'TELEGRAM' | 'EMAIL' | 'PARSER' | 'SUPPLIER' | 'GENERAL';
  isSecret: boolean;
  isEditable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentType {
  id: number;
  name: string;
  code: string;
  characteristics: EquipmentCharacteristic[];
  createdAt: string;
}

export interface EquipmentCharacteristic {
  id: number;
  equipmentTypeId: number;
  key: string;
  label: string;
  unit: string;
  sortOrder: number;
}

export interface Manufacturer {
  id: number;
  name: string;
  country: string;
  website: string;
  createdAt: string;
}

export interface EquipmentCatalogItem {
  id: number;
  equipmentType: EquipmentType;
  manufacturer: Manufacturer;
  modelName: string;
  modelNumber: string;
  specs: EquipmentCatalogSpec[];
  createdAt: string;
}

export interface EquipmentCatalogSpec {
  id: number;
  catalogItemId: number;
  characteristic: EquipmentCharacteristic;
  value: string;
}

export const CONFIG_GROUP_LABELS: Record<string, string> = {
  TELEGRAM: 'Telegram',
  EMAIL: 'Email',
  PARSER: 'Парсер',
  SUPPLIER: 'Поставщики',
  GENERAL: 'Общие',
};

export const CONFIG_GROUP_COLORS: Record<string, string> = {
  TELEGRAM: 'bg-info text-white',
  EMAIL: 'bg-warning text-dark',
  PARSER: 'bg-success text-white',
  SUPPLIER: 'bg-secondary text-white',
  GENERAL: 'bg-light text-dark border',
};

export const STATUS_LABELS: Record<string, string> = {
  NEW: 'Новый',
  PARSING: 'Парсинг',
  PARSED: 'Распарсен',
  SEARCHING_SUPPLIERS: 'Поиск поставщиков',
  SUPPLIERS_FOUND: 'Поставщики найдены',
  DOWNLOADING_DOCUMENTS: 'Загрузка документов',
  DOCUMENTS_ANALYZED: 'Документы проанализированы',
  EMAIL_SENT: 'Email отправлен',
  COMPLETED: 'Завершен',
  ERROR: 'Ошибка',
  PENDING: 'В ожидании',
  SEARCHING: 'Поиск',
  FOUND_ON_SUPPLIER: 'Найдено у поставщика',
  MODEL_MATCHED: 'Модель подтверждена',
  NOT_FOUND: 'Не найдено',
};

export const STATUS_CLASSES: Record<string, string> = {
  NEW: 'status-new',
  PARSING: 'status-parsing',
  PARSED: 'status-parsed',
  SEARCHING_SUPPLIERS: 'status-searching',
  SUPPLIERS_FOUND: 'status-found',
  DOWNLOADING_DOCUMENTS: 'status-downloading',
  DOCUMENTS_ANALYZED: 'status-analyzed',
  EMAIL_SENT: 'status-email-sent',
  COMPLETED: 'status-completed',
  ERROR: 'status-error',
  PENDING: 'bg-secondary',
  SEARCHING: 'status-parsing',
  FOUND_ON_SUPPLIER: 'status-found',
  MODEL_MATCHED: 'status-analyzed',
  NOT_FOUND: 'status-error',
};
