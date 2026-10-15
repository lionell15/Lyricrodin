#define _GNU_SOURCE
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include <dlfcn.h>
#include <link.h>
#include <android/log.h>
#include <sys/system_properties.h>
#define TAG "RodinDiscoverNative"
#define EXPECTED_CRC32 UINT32_C(0xf262e085)
#define TEXT_RVA ((uintptr_t)0x4c6d80)
#define TEXT_LEN ((size_t)0x8a6d54)
#define LIBRARY_NAME "libapp_launcher.so"
#define PROP_ENABLE "persist.rodin.discover.enable"
#define PROP_SITE "persist.rodin.discover.site"
typedef int (*HookFunType)(void *, void *, void **);
typedef int (*UnhookFunType)(void *);
typedef void (*NativeOnModuleLoaded)(const char *,void *);
typedef struct {uint32_t version; HookFunType hook_func; UnhookFunType unhook_func;} NativeAPIEntries;
static const uintptr_t sites[]={
0x53c8d0,0x5927e4,0x599d44,0x599fa0,0x599fbc,0x59a06c,0x59a088,
0x59c3e4,0x5cac6c,0x5cacf8,0x5f64e4,0x5f6500,0x5fdd5c,0x5fdde8,
0x606fac,0x626128,0x6265a8,0x626a44,0x62da30,0x658b8c,0x67669c,
0x6a3314,0x6d048c,0x6f84c4,0x6fcc64,0x6fd2ec,0x71c04c,0x816828,
0x824ef0,0x8273f0,0x827598,0x832be4,0x8336e8,0x83399c,0x833a44,
0x833bf4,0x833c78,0x833e0c,0x8342bc,0x8369b0,0x8d927c,0x8f3b64,
0x8f3bbc,0x8f4024,0x8f436c,0x905294,0xaa32dc};
#define SITE_COUNT (sizeof(sites)/sizeof(sites[0]))
static uintptr_t g_base;
static HookFunType g_hook;
static uint8_t (*g_original)(void *);
static uint64_t g_seen;
static volatile int g_installed;
static int endswith(const char *s,const char *suffix){
 if(!s||!suffix)return 0;
 size_t a=strlen(s),b=strlen(suffix);
 return a>=b && memcmp(s+a-b,suffix,b)==0;
}
static uint32_t crc32_bytes(const uint8_t *data,size_t len){
 uint32_t crc=~UINT32_C(0);
 for(size_t i=0;i<len;i++){
  crc^=data[i];
  for(int j=0;j<8;j++)crc=(crc>>1)^((crc&1)?UINT32_C(0xedb88320):0);
 }
 return ~crc;
}
static int find_lib(struct dl_phdr_info *info,size_t size,void *out){
 (void)size;
 if(info&&endswith(info->dlpi_name,LIBRARY_NAME)){
  *(uintptr_t *)out=(uintptr_t)info->dlpi_addr;
  return 1;
 }
 return 0;
}
static int find_site(uintptr_t addr){
 for(size_t i=0;i<SITE_COUNT;i++)if(sites[i]==addr)return (int)i;
 return -1;
}
static uintptr_t hex(const char *s){
 if(!s)return 0;
 if(s[0]=='0'&&(s[1]=='x'||s[1]=='X'))s+=2;
 uintptr_t n=0;
 int digits=0;
 for(;*s;s++){
  int d;
  if(*s>='0'&&*s<='9')d=*s-'0';
  else if(*s>='a'&&*s<='f')d=*s-'a'+10;
  else if(*s>='A'&&*s<='F')d=*s-'A'+10;
  else return 0;
  if(++digits>12)return 0;
  n=(n<<4)|(uintptr_t)d;
 }
 return digits?n:0;
}
__attribute__((noinline))
static uint8_t hooked(void *self){
 uintptr_t ret=(uintptr_t)__builtin_return_address(0);
 uintptr_t site=(g_base&&ret>=g_base+TEXT_RVA+4&&
                 ret<g_base+TEXT_RVA+TEXT_LEN+4)?ret-g_base-4:0;
 uint8_t orig=g_original?g_original(self):0;
 int idx=find_site(site);
 if(idx>=0){
  uint64_t bit=UINT64_C(1)<<(unsigned)idx;
  uint64_t old=__atomic_fetch_or(&g_seen,bit,__ATOMIC_RELAXED);
  if(!(old&bit))
   __android_log_print(ANDROID_LOG_INFO,TAG,
    "CALLSITE site=0x%lx original=%u",(unsigned long)site,(unsigned)orig);
  char enabled[PROP_VALUE_MAX]={0};
  char requested[PROP_VALUE_MAX]={0};
  __system_property_get(PROP_ENABLE,enabled);
  if(enabled[0]=='1'&&enabled[1]=='\0'){
   __system_property_get(PROP_SITE,requested);
   if(hex(requested)==site&&site!=0){
    __android_log_print(ANDROID_LOG_WARN,TAG,"FORCE site=0x%lx",
                       (unsigned long)site);
    return 1;
   }
  }
 }
 return orig;
}
static void loaded(const char *name,void *handle){
 if(!endswith(name,LIBRARY_NAME)||!handle||!g_hook||g_installed)return;
 uintptr_t base=0;
 dl_iterate_phdr(find_lib,&base);
 if(!base){
  __android_log_print(ANDROID_LOG_ERROR,TAG,"SKIP no ELF map");
  return;
 }
 uint32_t crc=crc32_bytes((const uint8_t *)(base+TEXT_RVA),TEXT_LEN);
 if(crc!=EXPECTED_CRC32){
  __android_log_print(ANDROID_LOG_ERROR,TAG,
    "SKIP CRC32=%08x expected=%08x",crc,EXPECTED_CRC32);
  return;
 }
 void *target=dlsym(handle,"MiuiOsBuild_is_international_build");
 if(!target){
  __android_log_print(ANDROID_LOG_ERROR,TAG,"SKIP symbol missing");
  return;
 }
 g_base=base;
 if(g_hook(target,(void *)hooked,(void **)&g_original)!=0||!g_original){
  g_base=0;
  __android_log_print(ANDROID_LOG_ERROR,TAG,"SKIP hook failed");
  return;
 }
 g_installed=1;
 __android_log_print(ANDROID_LOG_INFO,TAG,
    "READY passive trace CRC32=%08x, force off by default",crc);
}
__attribute__((visibility("default"),used))
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries){
 if(!entries||!entries->hook_func)return NULL;
 g_hook=entries->hook_func;
 __android_log_print(ANDROID_LOG_INFO,TAG,
  "native_init API=%u",entries->version);
 return loaded;
}
