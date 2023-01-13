import {
  useMemo,
  createContext,
  useContext,
  /* useEffect,
  useReducer, */
} from "react";

import { ACTION /* ID, IFolder, IResource */ } from "ode-ts-client";

import { useOdeContext } from "../OdeContext/OdeContext";
import {
  ExplorerContextProps,
  /* ThingWithAnID,
  ActionOnThingsWithAnId, */
  ExplorerProviderProps,
} from "./types";

const Context = createContext<ExplorerContextProps | null>(null!);

/**
 * These actions are used with selectionReducer
 */
/* const SELECT_ELEMENT = "SELECT_ELEMENT";
const DESELECT_ELEMENT = "DESELECT_ELEMENT";
const DESELECT_ALL = "DESELECT_ALL"; */

/**
 * This React Reducer is used to:
 * select folder or resource
 * deselect folder or resource
 * deselect all folders or resources.
 */
// function selectionReducer<T extends Record<ID, ThingWithAnID>>(
//   state: T,
//   action: ActionOnThingsWithAnId,
// ) {
//   switch (action.type) {
//     case SELECT_ELEMENT: {
//       const { payload } = action;
//       const id = payload?.id as string;

//       /* Add Object in Object with spread syntax and computed value */
//       return { ...state, [id]: { ...payload } };
//     }
//     case DESELECT_ELEMENT: {
//       const { payload } = action;
//       const id = payload?.id as string;

//       /* Remove Object from Object with spread syntax and computed value */
//       const { [id]: value, ...rest } = state;
//       return { ...rest };
//     }
//     case DESELECT_ALL: {
//       /* Reset state with empty Object */
//       return {};
//     }
//     default:
//       throw Error("Unknown action");
//   }
// }

/**
 * This React context is a wrapper for the ode-ts-client explorer framework. It
 * - initiates an exploring context,
 * - memoizes current folder,
 * - memoizes selected resources and folders,
 * - exports functions to select and deselect folders and resources,
 * - ...
 */
export default function ExplorerProvider({
  children,
  types,
}: ExplorerProviderProps) {
  const { params, explorer } = useOdeContext();

  // Exploration context
  const context = explorer.createContext(types, params.app);

  // Selected folders and resources
  /* const [selectedFolders, dispatchOnFolder] = useReducer(selectionReducer, {});
  const [selectedResources, dispatchOnResource] = useReducer(
    selectionReducer,
    {},
  ); */

  /* function selectFolder(folder: IFolder) {
    dispatchOnFolder({ type: "SELECT_ELEMENT", payload: folder });
  }

  function deselectFolder(folder: IFolder) {
    dispatchOnFolder({ type: "DESELECT_ELEMENT", payload: folder });
  }

  function deselectAllFolders() {
    dispatchOnFolder({ type: "DESELECT_ALL" });
  }

  function isFolderSelected(folder: IFolder) {
    return Object.hasOwn(selectedFolders, folder.id);
  }

  function selectResource(res: IResource) {
    dispatchOnResource({ type: "SELECT_ELEMENT", payload: res });
  }

  function deselectResource(res: IResource) {
    dispatchOnResource({ type: "DESELECT_ELEMENT", payload: res });
  }

  function deselectAllResources() {
    dispatchOnResource({ type: "DESELECT_ALL" });
  }

  function isResourceSelected(res: IResource) {
    return Object.hasOwn(selectedResources, res.id);
  } */

  /* async function openResource() {
    return await Promise.resolve(
      Object.values(selectedResources) as IResource[],
    )
      .then((items) => (items.length === 1 ? items[0] : null))
      .then(async (item) => {
        return !item
          ? await Promise.reject(new Error("Cannot open more than 1 resource"))
          : await explorer
              .getBus()
              .publish(types[0], ACTION.OPEN, { resourceId: item.assetId });
      });
  } */

  async function createResource() {
    return await explorer
      .getBus()
      .publish(types[0], ACTION.CREATE, "test proto");
  }

  const values = useMemo(
    () => ({
      context,
      explorer,
      /* selectedFolders: Object.values(selectedFolders) as IFolder[],
      selectedResources: Object.values(selectedResources) as IResource[],
      isFolderSelected,
      isResourceSelected, */
      /* openResource, */
      createResource,
      /* deselectAllFolders,
      deselectAllResources,
      deselectFolder,
      deselectResource,
      selectFolder,
      selectResource, */
    }),
    [],
  );

  return <Context.Provider value={values}>{children}</Context.Provider>;
}

export function useExplorerContext() {
  const context = useContext(Context);

  if (!context) {
    throw new Error(`Cannot be used outside of ExplorerContextProvider`);
  }
  return context;
}
