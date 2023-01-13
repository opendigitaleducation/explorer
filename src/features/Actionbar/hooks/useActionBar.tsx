import { useState, useEffect } from "react";

import { useExplorerContext } from "@contexts/ExplorerContext/ExplorerContext";
import { useModal } from "@ode-react-ui/hooks";
import { IAction, ACTION } from "ode-ts-client";

export default function useActionBar() {
  const [actions, setActions] = useState<IAction[]>([]);
  const [isActionBarOpen, setIsActionBarOpen] = useState<boolean>(false);

  const { toggle: toggleModal } = useModal(false);

  const {
    // openResource,
    createResource,
    context,
    // selectedResources,
    // selectedFolders,
  } = useExplorerContext();

  useEffect(() => {
    const ctx = context.getContext();
    if (ctx?.actions) {
      setActions(ctx.actions);
    }
  }, [context]);

  /* useEffect(() => {
    if (selectedResources.length === 0) {
      setIsActionBarOpen(false);
      return;
    }
    setIsActionBarOpen(true);
  }, [selectedResources]); */

  function handleOpenModal() {
    toggleModal(true);
  }

  function handleClick(action: IAction) {
    switch (action.id) {
      // case ACTION.OPEN:
      // return openResource();
      case ACTION.CREATE:
        return createResource();
      case ACTION.MOVE:
        return handleOpenModal();
      // case ACTION.SHARE:
      //   return explorer.onShare();
      // case ACTION.MANAGE:
      //   return explorer.onManage();
      default:
        throw Error("Unknown action: " + action.id);
    }
  }

  /**
   * Visibility rules for the action buttons.
   * @param action action to check
   * @returns true if the action button must be visible
   */
  function isActivable(action: IAction): boolean {
    /* const onlyOneItemSelected =
      selectedResources.length === 1 && selectedFolders.length === 0; */
    const onlyOneItemSelected = true;
    switch (action.id) {
      case ACTION.OPEN:
        return onlyOneItemSelected;
      case ACTION.SHARE:
        return onlyOneItemSelected;
      case ACTION.MANAGE:
        return onlyOneItemSelected;
      default:
        return true;
    }
  }

  return { actions, isActivable, handleClick, isActionBarOpen };
}
