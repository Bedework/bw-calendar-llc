package org.bedework.llc.common;

import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.configs.AuthProperties;
import org.bedework.calfacade.configs.SystemProperties;
import org.bedework.sysevents.events.SysEventBase;
import org.bedework.util.logging.Logged;

/**
 * This interface defines the interactions with the back end system
 * for a client.
 *
 * @author  Mike Douglass bedework.com
 */
public interface LowLevelClient extends Logged {
  /**
   * @param id   provide an id for logging and tracing.
   * @return a copy of this client which can be used for an asynchronous
   * action. Client copies should be discarded on completion of the request
   * cycle.
   */
  LowLevelClient copy(String id);

  /** Call on the way in once we have a client object.
   *
   * @param conversationType one off, initial, last or a continuation
   */
  void requestIn(int conversationType);

  /** Call on the way out.
   *
   * @param conversationType one off, initial, last or a continuation
   * @param actionType action or render
   * @param reqTimeMillis time for request.
   */
  void requestOut(int conversationType,
                  int actionType,
                  long reqTimeMillis);

  /**
   * @return boolean true if open
   */
  boolean isOpen();

  /** Call on the way out after handling a request..
   *
   */
  void close();

  /**
   *
   * @return current configuration
   */
  ConfigCommon getConf();

  /** Flush any backend data we may be hanging on to ready for a new
   * sequence of interactions. This is intended to help with web based
   * applications, especially those which follow the action/render url
   * pattern used in portlets.
   *
   * <p>A flushAll can discard a back end session allowing open to get a
   * fresh one. close() can then be either a no-op or something like a
   * hibernate disconnect.
   *
   * <p>This method should be called before calling open (or after calling
   * close).
   *
   */
  void flushAll();

  /** Send a notification event
   *
   * @param ev - system event
   */
  void postNotification(SysEventBase ev);

  /**
   * @return a change token for the current indexed data
   */
  String getCurrentChangeToken();

  /**
   *
   * @return true if we are doing public admin.
   */
  boolean getPublicAdmin();

  /**
   *
   * @return true if we are the web submit client.
   */
  boolean getWebSubmit();

  /**
   *
   * @return true for guest (read-only) mode.
   */
  boolean isGuest();

  /**
   *
   * @return true if we are the authenticated public client.
   */
  boolean getPublicAuth();

  /** apptype
   *
   * @return the client type
   */
  ClientTypes.ClientType getClientType();

  /** This may change as we switch groups.
   *
   * @return the current principal we are acting for
   */
  BwPrincipal<?> getCurrentPrincipal();

  /** This will not change.
   *
   * @return the principal we authenticated as
   */
  BwPrincipal<?> getAuthPrincipal();

  /** Return authentication relevant properties.
   *
   * @return AuthProperties object - never null.
   */
  AuthProperties getAuthProperties();

  /** Return properties about the system.
   *
   * @return SystemProperties object - never null.
   */
  SystemProperties getSystemProperties();

  /**
   * If possible roll back the changes.
   */
  void rollback();

  /**
   * @return System limit or user overrride - bytes.
   */
  long getUserMaxEntitySize();

  boolean isDefaultIndexPublic();

  /**
   * @param id of account
   * @param whoType - from WhoDefs
   * @return String principal uri
   */
  String makePrincipalUri(String id,
                          int whoType);

  /** Return principal for the given href.
   *
   * @param href of principal
   * @return Principal
   */
  BwPrincipal<?> getPrincipal(String href);
}
