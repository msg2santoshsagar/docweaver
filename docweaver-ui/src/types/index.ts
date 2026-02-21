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
