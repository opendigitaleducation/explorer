import { useExplorerContext } from "@contexts/index";
import useExplorerAdapter from "@features/Explorer/hooks/useExplorerAdapter";
import { useI18n } from "@hooks/useI18n";
import { Button, TreeView } from "@ode-react-ui/core";
import { Plus } from "@ode-react-ui/icons";

import useExplorer from "../../../store/store";
import useTreeView from "../hooks/useTreeView";

export const TreeViewContainer = () => {
  const { i18n } = useI18n();
  const { context } = useExplorerContext();
  const [state] = useExplorer();
  const { treeData } = state;
  /* feature treeview @hook */
  const { handleTreeItemFold, handleTreeItemSelect, handleTreeItemUnfold } =
    useTreeView(context, treeData);

  return (
    <>
      <TreeView
        data={treeData}
        onTreeItemSelect={handleTreeItemSelect}
        onTreeItemFold={handleTreeItemFold}
        onTreeItemUnfold={handleTreeItemUnfold}
      />
      <div className="d-grid my-16">
        <Button
          type="button"
          color="primary"
          variant="outline"
          leftIcon={<Plus />}
        >
          {i18n("blog.folder.new")}
        </Button>
      </div>
    </>
  );
};
