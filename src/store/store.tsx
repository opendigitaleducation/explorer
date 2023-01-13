import { useEffect, useReducer } from "react";

import { useExplorerContext } from "@contexts/index";
import TreeNodeFolderWrapper from "@features/Explorer/adapters/TreeNodeFolderWrapper";
import { TreeNode } from "@ode-react-ui/core";
import { IFolder, IResource } from "ode-ts-client";

/**
 * This hook acts as a data-model adapter.
 * It allows a TreeView component to explore and display folders streamed from an IExplorerContext.
 */
interface State {
  treeData: TreeNode;
  folders: IFolder[];
  listData: IResource[];
}

const initialState = {
  treeData: {
    id: "default",
    name: "Blogs",
    section: true,
    children: [],
  },
  folders: [],
  listData: [],
};

const reducer = (state: State = initialState, action: any) => {
  switch (action.type) {
    case "GET_RESOURCES": {
      const { resources } = action;
      console.log("resources", resources);

      return { ...state, listData: resources };
    }
    case "GET_FOLDERS": {
      const { folders } = action;
      return { ...state, folders };
    }
    case "GET_TREEDATA": {
      console.log("action", action);

      const { treeData } = action;
      return { ...state, ...treeData };
    }
    default:
      throw Error("Unknown action");
  }
};

export default function useExplorer() {
  const { context } = useExplorerContext();

  const [state, dispatch] = useReducer(reducer, initialState);

  // Observe streamed search results
  useEffect(() => {
    const subscription = context.latestResources().subscribe({
      next: (resultset) => {
        const { pagination } = context.getSearchParameters();
        pagination.maxIdx = resultset.output.pagination.maxIdx;
        pagination.startIdx =
          resultset.output.pagination.startIdx +
          resultset.output.pagination.pageSize;
        if (
          typeof pagination.maxIdx !== "undefined" &&
          pagination.startIdx > pagination.maxIdx
        ) {
          pagination.startIdx = pagination.maxIdx;
        }

        wrapTreeData(resultset?.output?.folders);
        wrapResourceData(resultset?.output?.resources);
        wrapFolderData(resultset?.output?.folders);
      },
    });

    return () => {
      if (subscription) {
        subscription.unsubscribe();
      }
    };
  }, []);

  function findNodeById(id: string, data: TreeNode): TreeNode | undefined {
    let res: TreeNode | undefined;
    if (data?.id === id) {
      return data;
    }
    if (data?.children?.length) {
      data?.children?.every((childNode: any) => {
        res = findNodeById(id, childNode);
        return res === undefined; // break loop if res is found
      });
    }
    return res;
  }

  function wrapTreeData(folders?: IFolder[]) {
    folders?.forEach((folder) => {
      const parentFolder = findNodeById(folder.parentId, state.treeData);
      if (
        !parentFolder?.children?.find((child: any) => child.id === folder.id)
      ) {
        if (parentFolder?.children) {
          parentFolder.children = [
            ...parentFolder.children,
            new TreeNodeFolderWrapper(folder),
          ];
        }
      }
    });

    dispatch({ type: "GET_TREEDATA", treeData: state.treeData });
  }

  function wrapResourceData(resources?: IResource[]) {
    if (resources?.length) {
      dispatch({ type: "GET_RESOURCES", resources });
    }
  }

  function wrapFolderData(folders?: IFolder[]) {
    if (folders?.length) {
      dispatch({ type: "GET_FOLDERS", folders });
    }
  }

  return [state];

  /* return useReducer(reducer, initialState); */
}
