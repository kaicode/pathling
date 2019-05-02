/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

import React, { useState } from "react";
import { ContextMenu, Menu } from "@blueprintjs/core";

import {
  getReverseReferences,
  ResourceNode,
  reverseReferences
} from "../fhir/ResourceTree";
import ContainedElements from "./ContainedElements";
import ReverseReference from "./ReverseReference";
import AddAggregation from "./AddAggregation";
import "./style/Resource.scss";
import TreeNodeTooltip from "./TreeNodeTooltip";

interface Props extends ResourceNode {
  name: string;
  parentPath?: string;
}

function Resource(props: Props) {
  const { name, definition, contains, parentPath } = props,
    [isExpanded, setExpanded] = useState(false);

  const openContextMenu = (event: any): void => {
    ContextMenu.show(
      <Menu>
        <AddAggregation path={name} />
      </Menu>,
      {
        left: event.clientX,
        top: event.clientY
      }
    );
  };

  const renderContains = () => {
    const newParentPath = parentPath ? parentPath : name,
      reverseReferenceNodes =
        name in reverseReferences
          ? getReverseReferences(name).map((node, i) => (
              <ReverseReference
                {...node}
                key={i + 1}
                parentPath={newParentPath}
              />
            ))
          : [];
    return [
      <ContainedElements key={0} nodes={contains} parentPath={newParentPath} />
    ].concat(reverseReferenceNodes);
  };

  return (
    <li className="resource">
      <TreeNodeTooltip type="Resource" definition={definition}>
        <span
          className={isExpanded ? "caret-open" : "caret-closed"}
          onClick={() => setExpanded(!isExpanded)}
        />
        <span className="icon" />
        <span className="label">{name}</span>
        {parentPath ? null : (
          <span className="action" onClick={openContextMenu} />
        )}
      </TreeNodeTooltip>
      {isExpanded ? <ol className="contains">{renderContains()}</ol> : null}
    </li>
  );
}

export default Resource;