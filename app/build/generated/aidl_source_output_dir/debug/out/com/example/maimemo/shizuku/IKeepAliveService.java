/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: C:\\Users\\18870\\AppData\\Local\\Android\\Sdk\\build-tools\\36.0.0\\aidl.exe -pC:\\Users\\18870\\AppData\\Local\\Android\\Sdk\\platforms\\android-36.1\\framework.aidl -oD:\\wxin\\xwechat_files\\wxid_ll9hmodctfn122_de4a\\msg\\file\\2026-10\\MaiMemoUI\\app\\build\\generated\\aidl_source_output_dir\\debug\\out -ID:\\wxin\\xwechat_files\\wxid_ll9hmodctfn122_de4a\\msg\\file\\2026-10\\MaiMemoUI\\app\\src\\main\\aidl -ID:\\wxin\\xwechat_files\\wxid_ll9hmodctfn122_de4a\\msg\\file\\2026-10\\MaiMemoUI\\app\\src\\debug\\aidl -IC:\\Users\\18870\\.gradle\\caches\\9.4.1\\transforms\\404d500152e2ac90318f53a7f6a474c9\\transformed\\core-1.16.0\\aidl -IC:\\Users\\18870\\.gradle\\caches\\9.4.1\\transforms\\d93e37f3187af41acd1dcff752f4d57a\\transformed\\versionedparcelable-1.1.1\\aidl -dC:\\Users\\18870\\AppData\\Local\\Temp\\aidl1094522207439667727.d D:\\wxin\\xwechat_files\\wxid_ll9hmodctfn122_de4a\\msg\\file\\2026-10\\MaiMemoUI\\app\\src\\main\\aidl\\com\\example\\maimemo\\shizuku\\IKeepAliveService.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.example.maimemo.shizuku;
public interface IKeepAliveService extends android.os.IInterface
{
  /** Default implementation for IKeepAliveService. */
  public static class Default implements com.example.maimemo.shizuku.IKeepAliveService
  {
    @Override public java.lang.String addDeviceIdleWhitelist(java.lang.String packageName) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.example.maimemo.shizuku.IKeepAliveService
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.example.maimemo.shizuku.IKeepAliveService interface,
     * generating a proxy if needed.
     */
    public static com.example.maimemo.shizuku.IKeepAliveService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.example.maimemo.shizuku.IKeepAliveService))) {
        return ((com.example.maimemo.shizuku.IKeepAliveService)iin);
      }
      return new com.example.maimemo.shizuku.IKeepAliveService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_addDeviceIdleWhitelist:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _result = this.addDeviceIdleWhitelist(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.example.maimemo.shizuku.IKeepAliveService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public java.lang.String addDeviceIdleWhitelist(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addDeviceIdleWhitelist, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_addDeviceIdleWhitelist = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.example.maimemo.shizuku.IKeepAliveService";
  public java.lang.String addDeviceIdleWhitelist(java.lang.String packageName) throws android.os.RemoteException;
}
