import { useEffect } from "react";

import { useExplorerContext } from "@contexts/index";
import { IFolder, IResource } from "ode-ts-client";

import useExplorer from "../../../store/store";
import { TreeNodeFolderWrapper } from "../adapters";
import { TreeNode } from "../types";

export default function useExplorerAdapter() {
  const { context } = useExplorerContext();

  const [state, dispatch] = useExplorer();

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
  }, []); // execute effect only once

  // TODO  const selectedNode = treeData;

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
      const parentFolder = findNodeById(folder.parentId, treeData);
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

    dispatch({ type: "GET_TREEDATA", treeData });
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

  const { treeData, listData, folders } = state;

  return {
    folders,
    treeData,
    listData,
  };
}
