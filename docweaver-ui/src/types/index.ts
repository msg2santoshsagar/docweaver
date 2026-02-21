export type ImageMode = 'STANDALONE' | 'GROUPED';
export type OutputType = 'IMAGE' | 'PDF';

export interface ImageAsset {
  id: string;
  originalFileName: string;
  displayName: string;
  mimeType: string;
  originalPath: string;
  fileSize: number;
  mode: ImageMode;
  uploadedAt: string;
}

export interface DocumentGroup {
  id: string;
  name: string;
  images: ImageAsset[];
  rotationByImageId: Record<string, number>;
  createdAt: string;
  updatedAt: string;
}

export interface AppConfig {
  outputFolder: string;
  defaultStandaloneOutputType: OutputType;
  defaultDeleteOriginals: boolean;
  dryRun: boolean;
  aiEnabled: boolean;
  aiModel: string;
  aiBaseUrl: string;
}

export interface GeneratedDocument {
  id: string;
  type: string;
  sourceImageId?: string;
  sourceGroupId?: string;
  outputPath: string;
  outputName: string;
  status: 'SUCCESS' | 'FAILED';
  deleteOriginals: boolean;
  dryRun: boolean;
  message: string;
  createdAt: string;
}

export interface ProcessResult {
  generatedDocumentId: string;
  type: string;
  sourceImageId?: string;
  sourceGroupId?: string;
  outputPath: string;
  outputName: string;
  status: 'SUCCESS' | 'FAILED';
  message: string;
}

export interface ProcessResponse {
  success: boolean;
  deleteOriginalsAttempted: boolean;
  originalsDeleted: boolean;
  dryRun: boolean;
  results: ProcessResult[];
}

export interface NameSuggestion {
  suggestedName: string;
  confidence: number;
  source: string;
  docType?: string;
  subject?: string;
  documentDate?: string;
  groupKey?: string;
}

export interface AiStatus {
  enabled: boolean;
  available: boolean;
  configuredModel: string;
  endpoint: string;
  reason: string;
  availableModels: string[];
}

export interface AutoCategorizeResponse {
  standaloneImageIds: string[];
  groups: {
    name: string;
    imageIds: string[];
    reason: string;
    confidence: number;
  }[];
  renamedImages: {
    imageId: string;
    previousName: string;
    newName: string;
    source: string;
  }[];
}
