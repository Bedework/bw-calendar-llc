package org.bedework.llc.ro;

import org.bedework.calfacade.BwGroup;
import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.configs.AuthProperties;
import org.bedework.calfacade.configs.SystemProperties;
import org.bedework.calfacade.indexing.BwIndexer;
import org.bedework.calfacade.svc.BwCalSuite;
import org.bedework.calfacade.svc.CalSvcIPars;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.llc.common.ClientTypes;
import org.bedework.llc.common.ConfigCommon;
import org.bedework.llc.common.LowLevelClient;
import org.bedework.sysevents.events.HttpEvent;
import org.bedework.sysevents.events.HttpOutEvent;
import org.bedework.sysevents.events.SysEventBase;
import org.bedework.util.logging.BwLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.bedework.calfacade.indexing.BwIndexer.docTypeCollection;
import static org.bedework.calfacade.indexing.BwIndexer.docTypeEvent;
import static org.bedework.util.servlet.ActionTypes.actionTypeAction;
import static org.bedework.util.servlet.ConversationTypes.conversationTypeEnd;
import static org.bedework.util.servlet.ConversationTypes.conversationTypeOnly;
import static org.bedework.util.servlet.ConversationTypes.conversationTypeProcessAndOnly;
import static org.bedework.util.servlet.ConversationTypes.conversationTypeUnknown;

public class ReadOnlyLowLevelClientImpl
        implements ReadOnlyLowLevelClient {
  protected ConfigCommon conf;

  protected String id;

  protected CalSvcIPars pars;

  protected CalSvcI svci;

  protected boolean publicView;

  protected BwPrincipal<?> currentPrincipal;

  protected ClientTypes.ClientType clientType;

  protected String calSuiteName;

  private final Map<String, BwIndexer> publicIndexers = new HashMap<>();
  private final Map<String, BwIndexer> userIndexers = new HashMap<>();

  /* Set this whenever an update occurs. We may want to delay or flush
   */
  protected long lastUpdate;

  /* Don't delay or flush until after end of request in which we
     updated.
   */
  protected long requestEnd;

  /** For copy
   *
   * @param conf client configuration
   * @param id identify the client - usually module name
   */
  protected ReadOnlyLowLevelClientImpl(final ConfigCommon conf,
                                       final String id) {
    this.id = id;
    this.conf = conf;
  }

  @Override
  public LowLevelClient copy(final String id) {
    final ReadOnlyLowLevelClientImpl cl =
            new ReadOnlyLowLevelClientImpl(conf, id);

    copyCommon(id, cl);

    cl.publicView = publicView;

    return cl;
  }

  @Override
  public void requestIn(final int conversationType) {
    postNotification(new HttpEvent(SysEventBase.SysCode.WEB_IN));
    svci.setState("Request in");

    if (conversationType == conversationTypeUnknown) {
      svci.open();
      svci.beginTransaction();
      return;
    }

    if (svci.isRolledback()) {
      svci.close();
    }

    if (conversationType == conversationTypeOnly) {
              /* if a conversation is already started on entry, end it
                  with no processing of changes. */
      if (svci.isOpen()) {
        svci.setState("Request in - close");
        svci.endTransaction();
      }
    }

    if (conversationType == conversationTypeProcessAndOnly) {
      if (svci.isOpen()) {
        svci.setState("Request in - flush");
        svci.flushAll();
        svci.endTransaction();
        svci.close();
      }
    }

    svci.open();
    svci.beginTransaction();
    svci.setState("Request in - started");
  }

  @Override
  public void requestOut(final int conversationType,
                         final int actionType,
                         final long reqTimeMillis) {
    requestEnd = System.currentTimeMillis();
    postNotification(
            new HttpOutEvent(SysEventBase.SysCode.WEB_OUT,
                             reqTimeMillis));
    svci.setState("Request out");
    publicIndexers.clear();
    userIndexers.clear();

    if (!isOpen()) {
      return;
    }

    if (conversationType == conversationTypeUnknown) {
      if (actionType != actionTypeAction) {
        flushAll();
      }
    } else {
      if ((conversationType == conversationTypeEnd) ||
              (conversationType == conversationTypeOnly)) {
        flushAll();
      }
    }

    svci.endTransaction();
    svci.setState("Request out - ended");
  }

  @Override
  public boolean isOpen() {
    return svci.isOpen();
  }

  @Override
  public void close() {
    svci.close();
  }

  @Override
  public ConfigCommon getConf() {
    return conf;
  }

  @Override
  public void flushAll() {
    svci.flushAll();
  }

  @Override
  public void postNotification(final SysEventBase ev) {
    svci.postNotification(ev);
  }

  @Override
  public String getCurrentChangeToken() {
    var evChg = getIndexer(isDefaultIndexPublic(),
                           docTypeEvent).currentChangeToken();
    if (evChg == null) {
      evChg = "";
    }

    return evChg + getIndexer(isDefaultIndexPublic(),
                              docTypeCollection).currentChangeToken();
  }

  @Override
  public boolean getPublicAdmin() {
    return false;
  }

  @Override
  public boolean getWebSubmit() {
    return false;
  }

  @Override
  public boolean isGuest() {
    return true;
  }

  @Override
  public boolean getPublicAuth() {
    return ClientTypes.ClientType.publicAuth == clientType;
  }

  @Override
  public ClientTypes.ClientType getClientType() {
    return clientType;
  }

  @Override
  public BwPrincipal<?> getCurrentPrincipal() {
    if (currentPrincipal == null) {
      currentPrincipal = (BwPrincipal<?>)svci.getPrincipal().clone();
    }

    return currentPrincipal;
  }

  @Override
  public BwPrincipal<?> getAuthPrincipal() {
    return svci.getPrincipalInfo().getAuthPrincipal();
  }

  @Override
  public AuthProperties getAuthProperties() {
    return svci.getAuthProperties();
  }

  @Override
  public SystemProperties getSystemProperties() {
    return svci.getSystemProperties();
  }

  @Override
  public void rollback() {
    try {
      svci.rollbackTransaction();
    } catch (final Throwable ignored) {}

    try {
      svci.endTransaction();
    } catch (final Throwable ignored) {}
  }

  @Override
  public long getUserMaxEntitySize() {
    return svci.getUserMaxEntitySize();
  }

  @Override
  public boolean isDefaultIndexPublic() {
    return getWebSubmit() || getPublicAdmin() || isGuest();
  }

  @Override
  public String makePrincipalUri(final String id,
                                 final int whoType) {
    return svci.getDirectories().makePrincipalUri(id, whoType);
  }

  @Override
  public BwPrincipal<?> getPrincipal(final String href)
  {
    return svci.getDirectories().getPrincipal(href);
  }

  @Override
  public Collection<BwGroup<?>> getAdminGroups(final boolean populate) {
    return svci.getAdminDirectories().getAll(populate);
  }

  @Override
  public Collection<BwCalSuite> getCalSuites() {
    return svci.getCalSuitesHandler().getAll();
  }

  @Override
  public Collection<BwGroup<?>> getAllAdminGroups(
          final BwPrincipal<?> val) {
    return svci.getAdminDirectories().getAllGroups(val);
  }

  protected void copyCommon(final String id,
                            final ReadOnlyLowLevelClientImpl cl) {
    cl.pars = (CalSvcIPars)pars.clone();
    cl.pars.setLogId(id);

    cl.svci = new CalSvcFactoryDefault().getSvc(cl.pars);
    cl.clientType = clientType;
    cl.calSuiteName = calSuiteName;
  }

  protected BwIndexer getIndexer(final boolean publick,
                                 final String docType) {
    if (publick) {
      BwIndexer idx = publicIndexers.get(docType);
      if (idx == null) {
        idx = svci.getIndexer(true, docType);
        publicIndexers.put(docType, idx);
      }

      return idx;
    }

    BwIndexer idx = userIndexers.get(docType);
    if (idx == null) {
      idx = svci.getIndexer(false, docType);
      userIndexers.put(docType, idx);
    }

    return idx;
  }

  /* ============================================================
   *                   Logged methods
   * ============================================================ */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
