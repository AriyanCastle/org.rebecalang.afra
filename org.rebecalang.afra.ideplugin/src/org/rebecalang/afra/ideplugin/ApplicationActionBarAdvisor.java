package org.rebecalang.afra.ideplugin;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.menus.IMenuService;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private boolean isDisposed;
    private IWorkbenchWindow window;
    private MenuManager coolbarPopupMenuManager;
    private StatusLineContributionItem statusLineItem;
    
//    private IWorkbenchAction fileNewAction;
//    private IWorkbenchAction fileCloseAction;
//    private IWorkbenchAction fileCloseAllAction;
//    private IWorkbenchAction fileCloseOthersAction;
//    private IWorkbenchAction fileSaveAction;
//    private IWorkbenchAction fileSaveAllAction;
//    private IWorkbenchAction fileImportResourcesAction;
//    private IWorkbenchAction fileExportResourcesAction;
//    private IWorkbenchAction filePropertiesAction;
//    private IWorkbenchAction fileQuitAction;
//
//    private IWorkbenchAction editUndoAction;
//    private IWorkbenchAction editRedoAction;
//    private IWorkbenchAction editCutAction;
//    private IWorkbenchAction editCopyAction;
//    private IWorkbenchAction editPasteAction;
//    private IWorkbenchAction editDeleteAction;
//    private IWorkbenchAction editSelectAllAction;
//    private IWorkbenchAction editFindAction;
//
//    private IWorkbenchAction winNewWindowAction;
//    private MenuManager winOpenOperspectiveMenu;
//    private MenuManager winShowViewMenu;
//    private IWorkbenchAction winEditActionSetAction;
//    private IWorkbenchAction winSavePerspectiveAction;
//    private IWorkbenchAction winResetPerspectiveAction;
//    private IWorkbenchAction winClosePerspectiveAction;
//    private IWorkbenchAction winCloseAllPerspectivesAction;
//    private MenuManager winShortcutsMenu;
//    private IWorkbenchAction winOpenPreferencesAction;
//    private IContributionItem winOpenWindowsItem;
//
//    private IWorkbenchAction helpContentsAction;
//    private IWorkbenchAction helpSearchAction;
//    private IWorkbenchAction helpDynamicHelpAction;
//    private IWorkbenchAction helpAboutAction;
//
//    private IWorkbenchAction scutShowPartPaneMenuAction;
//    private IWorkbenchAction scutShowViewMenuAction;
//    private IWorkbenchAction scutMaximizePartAction;
//    private IWorkbenchAction scutMinimizePartAction;
//    private IWorkbenchAction scutActivateEditorAction;
//    private IWorkbenchAction scutNextEditorAction;
//    private IWorkbenchAction scutPrevEditorAction;
//    private IWorkbenchAction scutSwitchToEditorAction;
//    private IWorkbenchAction scutNextPartAction;
//    private IWorkbenchAction scutPrevPartAction;
//    private IWorkbenchAction scutNextPerspectiveAction;
//    private IWorkbenchAction scutPrevPerspectiveAction;
//
//    private IWorkbenchAction hideShowEditorAction;
//    private IWorkbenchAction lockToolBarAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
        this.window = configurer.getWindowConfigurer().getWindow();
    }

    @Override
    public void dispose()
    {
        if (this.isDisposed)
        {
            return;
        }
        this.isDisposed = true;
        IMenuService menuService = (IMenuService) this.window.getService(IMenuService.class);
        menuService.releaseContributions(this.coolbarPopupMenuManager);
        this.coolbarPopupMenuManager.dispose();
        getActionBarConfigurer().getStatusLineManager().remove(this.statusLineItem);
        super.dispose();
    }

	@Override
    protected void makeActions(final IWorkbenchWindow window)
    {
        super.makeActions(window);
        /*
        this.statusLineItem = new StatusLineContributionItem("ModeContributionItem"); //$NON-NLS-1$

        register(this.fileNewAction = ActionFactory.NEW_WIZARD_DROP_DOWN.create(window));
        register(this.fileCloseAction = ActionFactory.CLOSE.create(window));
        register(this.fileCloseAllAction = ActionFactory.CLOSE_ALL.create(window));
        register(this.fileCloseOthersAction = ActionFactory.CLOSE_OTHERS.create(window));
        register(this.fileSaveAction = ActionFactory.SAVE.create(window));
        register(this.fileSaveAllAction = ActionFactory.SAVE_ALL.create(window));
        register(this.fileImportResourcesAction = ActionFactory.IMPORT.create(window));
        register(this.fileExportResourcesAction = ActionFactory.EXPORT.create(window));
        register(this.filePropertiesAction = ActionFactory.PROPERTIES.create(window));
        register(this.fileQuitAction = ActionFactory.QUIT.create(window));

        register(this.editUndoAction = ActionFactory.UNDO.create(window));
        register(this.editRedoAction = ActionFactory.REDO.create(window));
        register(this.editCutAction = ActionFactory.CUT.create(window));
        register(this.editCopyAction = ActionFactory.COPY.create(window));
        register(this.editPasteAction = ActionFactory.PASTE.create(window));
        register(this.editDeleteAction = ActionFactory.DELETE.create(window));
        register(this.editSelectAllAction = ActionFactory.SELECT_ALL.create(window));
        register(this.editFindAction = ActionFactory.FIND.create(window));

        register(this.winNewWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window));
        this.winOpenOperspectiveMenu = createSubMenu("&Open Perspective", "openPerspective", ContributionItemFactory.PERSPECTIVES_SHORTLIST);
        this.winShowViewMenu = createSubMenu("Show &View", "showView", ContributionItemFactory.VIEWS_SHORTLIST);
        register(this.winEditActionSetAction = ActionFactory.EDIT_ACTION_SETS.create(window));
        register(this.winSavePerspectiveAction = ActionFactory.SAVE_PERSPECTIVE.create(window));
        register(this.winResetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window));
        register(this.winClosePerspectiveAction = ActionFactory.CLOSE_PERSPECTIVE.create(window));
        register(this.winCloseAllPerspectivesAction = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window));
        register(this.winOpenPreferencesAction = ActionFactory.PREFERENCES.create(window));
        this.winOpenWindowsItem = ContributionItemFactory.OPEN_WINDOWS.create(this.window);

        register(this.scutShowPartPaneMenuAction = ActionFactory.SHOW_PART_PANE_MENU.create(window));
        register(this.scutShowViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window));
        register(this.scutMaximizePartAction = ActionFactory.MAXIMIZE.create(window));
        register(this.scutMinimizePartAction = ActionFactory.MINIMIZE.create(window));
        register(this.scutActivateEditorAction = ActionFactory.ACTIVATE_EDITOR.create(window));
        register(this.scutNextEditorAction = ActionFactory.NEXT_EDITOR.create(window));
        register(this.scutPrevEditorAction = ActionFactory.PREVIOUS_EDITOR.create(window));
        ActionFactory.linkCycleActionPair(this.scutNextEditorAction, this.scutPrevEditorAction);
        register(this.scutSwitchToEditorAction = ActionFactory.SHOW_OPEN_EDITORS.create(window));
        register(this.scutNextPartAction = ActionFactory.NEXT_PART.create(window));
        register(this.scutPrevPartAction = ActionFactory.PREVIOUS_PART.create(window));
        ActionFactory.linkCycleActionPair(this.scutNextPartAction, this.scutPrevPartAction);
        register(this.scutNextPerspectiveAction = ActionFactory.NEXT_PERSPECTIVE.create(window));
        register(this.scutPrevPerspectiveAction = ActionFactory.PREVIOUS_PERSPECTIVE.create(window));
        ActionFactory.linkCycleActionPair(this.scutNextPerspectiveAction, this.scutPrevPerspectiveAction);

        this.winShortcutsMenu = createSubMenu("Navi&gation", "shortcuts",
                this.scutShowPartPaneMenuAction,
                this.scutShowViewMenuAction,
                new Separator(),
                this.scutMaximizePartAction,
                this.scutMinimizePartAction,
                new Separator(),
                this.scutActivateEditorAction,
                this.scutNextEditorAction,
                this.scutPrevEditorAction,
                this.scutSwitchToEditorAction,
                new Separator(),
                this.scutNextPartAction,
                this.scutPrevPartAction,
                new Separator(),
                this.scutNextPerspectiveAction,
                this.scutPrevPerspectiveAction
                );

        register(this.helpContentsAction = ActionFactory.HELP_CONTENTS.create(window));
        register(this.helpSearchAction = ActionFactory.HELP_SEARCH.create(window));
        register(this.helpDynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window));
        this.helpAboutAction = ActionFactory.ABOUT.create(window);
        this.helpAboutAction.setImageDescriptor(window.getWorkbench().getSharedImages().getImageDescriptor("IMG_OBJS_DEFAULT_PRO"));
        register(this.helpAboutAction);

        this.hideShowEditorAction = ActionFactory.SHOW_EDITOR.create(window);
        register(this.hideShowEditorAction);
        this.lockToolBarAction = ActionFactory.LOCK_TOOL_BAR.create(window);
        register(this.lockToolBarAction);
*/
    }

	/*
    @Override
    protected void fillStatusLine(IStatusLineManager statusLine)
    {
        statusLine.add(this.statusLineItem);
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar)
    {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
        fileMenu.add(new Separator());
        fileMenu.add(this.fileCloseAction);
        fileMenu.add(this.fileCloseAllAction);
        fileMenu.add(this.fileCloseOthersAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
        fileMenu.add(new Separator());
        fileMenu.add(this.fileSaveAction);
        fileMenu.add(this.fileSaveAllAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.PRINT_EXT));
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.OPEN_EXT));
        fileMenu.add(this.fileImportResourcesAction);
        fileMenu.add(this.fileExportResourcesAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.IMPORT_EXT));
        fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        fileMenu.add(this.filePropertiesAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
        fileMenu.add(new Separator());
        fileMenu.add(this.fileQuitAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        menuBar.add(fileMenu);

        MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
        editMenu.add(this.editUndoAction);
        editMenu.add(this.editRedoAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
        editMenu.add(new Separator());
        editMenu.add(this.editCutAction);
        editMenu.add(this.editCopyAction);
        editMenu.add(this.editPasteAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
        editMenu.add(new Separator());
        editMenu.add(this.editDeleteAction);
        editMenu.add(this.editSelectAllAction);
        editMenu.add(new Separator());
        editMenu.add(this.editFindAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.ADD_EXT));
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
        editMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(editMenu);

        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        MenuManager windMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        windMenu.add(this.winNewWindowAction);
        windMenu.add(new Separator());
        windMenu.add(this.winOpenOperspectiveMenu);
        windMenu.add(this.winShowViewMenu);
        windMenu.add(new Separator());
        windMenu.add(this.winEditActionSetAction);
        windMenu.add(this.winSavePerspectiveAction);
        windMenu.add(this.winResetPerspectiveAction);
        windMenu.add(this.winClosePerspectiveAction);
        windMenu.add(this.winCloseAllPerspectivesAction);
        windMenu.add(new Separator());
        windMenu.add(this.winShortcutsMenu);
        windMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        windMenu.add(this.winOpenPreferencesAction);
        windMenu.add(this.winOpenWindowsItem);
        menuBar.add(windMenu);

        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
        helpMenu.add(new GroupMarker("group.intro"));
        helpMenu.add(new GroupMarker("group.intro.ext"));
        helpMenu.add(new GroupMarker("group.main"));
        helpMenu.add(this.helpContentsAction);
        helpMenu.add(this.helpSearchAction);
        helpMenu.add(this.helpDynamicHelpAction);
        helpMenu.add(new GroupMarker("group.assist"));
        helpMenu.add(new GroupMarker("group.main.ext"));
        helpMenu.add(new GroupMarker("group.tutorials"));
        helpMenu.add(new GroupMarker("group.tools"));
        helpMenu.add(new GroupMarker("group.updates"));
        helpMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(new GroupMarker("group.about"));
        helpMenu.add(this.helpAboutAction);
        helpMenu.add(new GroupMarker("group.about.ext"));
        menuBar.add(helpMenu);
    }
    
    
    @Override
    protected void fillCoolBar(ICoolBarManager coolBar){
    	IMenuService menuService = (IMenuService) this.window.getService(IMenuService.class);

        this.coolbarPopupMenuManager = new MenuManager();
        this.coolbarPopupMenuManager.add(new ActionContributionItem(this.lockToolBarAction));
        this.coolbarPopupMenuManager.add(new ActionContributionItem(this.winEditActionSetAction));
        coolBar.setContextMenuManager(this.coolbarPopupMenuManager);
        menuService.populateContributionManager(this.coolbarPopupMenuManager, "popup:windowCoolbarContextMenu");

        IToolBarManager toolbar = new ToolBarManager(SWT.RIGHT);
        toolbar.add(this.fileNewAction);

        coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_FILE));
        coolBar.add(new ToolBarContributionItem(toolbar, IWorkbenchActionConstants.TOOLBAR_FILE));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_EDITOR));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_HELP));
    }

    

    protected MenuManager createSubMenu(String text, String id, ContributionItemFactory factory)
    {
        MenuManager submenu = new MenuManager(text, id);
        submenu.add(factory.create(this.window));
        return submenu;
    }

    protected MenuManager createSubMenu(String text, String id, Object... items)
    {
        MenuManager submenu = new MenuManager(text, id);
        for (Object i : items)
        {
            if (i instanceof IContributionItem)
                submenu.add((IContributionItem) i);
            else if (i instanceof IWorkbenchAction)
                submenu.add((IWorkbenchAction) i);
        }
        return submenu;
    }
    */
}
