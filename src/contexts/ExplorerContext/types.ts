import { ReactNode } from "react";

import {
  ResourceType,
  IFolder,
  IResource,
  IExplorerContext,
  IExplorerFramework,
} from "ode-ts-client";

export interface ExplorerProviderProps {
  children: ReactNode;
  types: ResourceType[];
}

/** The resources/folders selection reducer */
export type ThingWithAnID = IFolder | IResource;
export interface ActionOnThingsWithAnId {
  type: string;
  payload?: ThingWithAnID;
}

export interface ExplorerContextProps {
  context: IExplorerContext;
  explorer: IExplorerFramework;
  /* selectedFolders: IFolder[];
  selectedResources: IResource[];
  isFolderSelected: (folder: IFolder) => boolean;
  isResourceSelected: (res: IResource) => boolean; */
  createResource: () => void;
  openResource: () => void;
  /* selectFolder: (folder: IFolder) => void;
  selectResource: (res: IResource) => void;
  deselectAllFolders: () => void;
  deselectAllResources: () => void;
  deselectFolder: (folder: IFolder) => void;
  deselectResource: (res: IResource) => void; */
}
