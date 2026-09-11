#!/usr/bin/env bash
#
# SkinDemo 一键打包脚本：换肤 SDK（AAR）+ 主题皮肤包（.skin）
#
# 用法：
#   ./package.sh            # 默认打包全部（sdk + skin）
#   ./package.sh sdk        # 仅打包换肤 SDK → dist/skinlibrary-release-<版本>-<日期时间>-<githash>.aar
#   ./package.sh skin       # 仅打包主题皮肤包 → dist/skindemo-<版本>-<日期时间>-<githash>.skin
#   ./package.sh byd        # 仅打包比亚迪控件换肤桥 → dist/bydwidget-release-<版本>-<日期时间>-<githash>.aar
#   ./package.sh clean      # 清理本次构建产物（dist/）
#
# 说明：
#   1. 「换肤 SDK」是 Android library，含 R 资源与 Skinnable* 控件，必须打成 AAR
#      （纯 .jar 不含资源，下游宿主无法做同名资源映射换肤）。
#   2. 「主题皮肤包」是独立 application 模块，打包成 APK 后改名 .skin；
#      宿主通过反射 AssetManager.addAssetPath 加载，故无需签名。
#   3. 分发产物命名含「版本 + 日期 + 时间 + git 短哈希」，每次执行先删同类旧产物再重建。
#      版本号默认取 git 最近的 tag，无 tag 时回退 1.0.0；可用 VERSION=xx 覆盖。
#   4. 皮肤包同时回填到 app/src/main/assets/skin/skindemo.skin（固定名，宿主启动
#      时 AssetsUtils.doCopy 按此名加载），回填名不携带时间戳。皮肤是宿主资源，
#      主题库（skinlibrary）不内置皮肤。
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

GRADLEW="$ROOT/gradlew"

# ---------- 版本 / 日期 / 时间 / git 哈希（用于产物命名） ----------
# 版本号：优先 VERSION 环境变量 → git 最近 tag → 1.0.0
if [ -n "${VERSION:-}" ]; then
    VERSION="$VERSION"
elif git_tag="$(git describe --tags --abbrev=0 2>/dev/null)"; then
    VERSION="$git_tag"
else
    VERSION="1.0.0"
fi
BUILD_TIME="$(date +%Y%m%d-%H%M%S)"
GIT_REV="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
STAMP="${VERSION}-${BUILD_TIME}-${GIT_REV}"

# 皮肤包 assets 回填名（固定，宿主按此名加载；不加时间戳）
ASSETS_SKIN_NAME="${SKIN_NAME:-skindemo.skin}"

DIST_DIR="$ROOT/dist"

SKIN_LIB_MODULE=":skinlibrary"
SKIN_PKG_MODULE=":skinpackage"
BYD_MODULE=":bydwidget"

SKIN_LIB_AAR="$ROOT/skinlibrary/build/outputs/aar/skinlibrary-release.aar"
SKIN_PKG_APK="$ROOT/skinpackage/build/outputs/apk/release/skinpackage-release-unsigned.apk"
BYD_AAR="$ROOT/bydwidget/build/outputs/aar/bydwidget-release.aar"
ASSETS_SKIN_DIR="$ROOT/app/src/main/assets/skin"

# ---------- 输出辅助 ----------
if [ -t 1 ]; then
    C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'; C_RESET=$'\033[0m'
else
    C_GREEN=""; C_YELLOW=""; C_RED=""; C_RESET=""
fi

log_info() { printf '%s\n' "${C_YELLOW}[INFO]${C_RESET} $*"; }
log_ok()   { printf '%s\n' "${C_GREEN}[ OK ]${C_RESET} $*"; }
log_err()  { printf '%s\n' "${C_RED}[FAIL]${C_RESET} $*" >&2; }

need_gradlew() {
    if [ ! -x "$GRADLEW" ] && [ ! -f "$GRADLEW" ]; then
        log_err "找不到 gradlew：$GRADLEW"
        exit 1
    fi
}

ensure_dist() { mkdir -p "$DIST_DIR"; }

# 删除匹配 glob 的旧产物（每次打包先删同类旧文件再重建，时间戳不同也不残留）
rm_old_glob() {
    local glob="$1"
    local label="$2"
    if compgen -G "$glob" >/dev/null 2>&1; then
        rm -f $glob
        log_info "删除旧产物：$label（$glob）"
    fi
}

# ---------- 打包换肤 SDK（AAR） ----------
build_sdk() {
    log_info "打包换肤 SDK（AAR）…"
    rm_old_glob "$DIST_DIR/skinlibrary-release-*.aar" "换肤 SDK"

    "$GRADLEW" "$SKIN_LIB_MODULE:assembleRelease"

    local aar="$SKIN_LIB_AAR"
    [ -f "$aar" ] || { log_err "未生成 AAR：$aar"; exit 1; }

    ensure_dist
    local out="$DIST_DIR/skinlibrary-release-${STAMP}.aar"
    cp -f "$aar" "$out"
    log_ok "换肤 SDK → $out"
}

# ---------- 打包主题皮肤包（.skin） ----------
build_skin() {
    log_info "打包主题皮肤包（APK → .skin）…"
    # 删 dist 与 assets 里所有旧皮肤包（时间戳命名会变，按 *.skin 全清）
    rm_old_glob "$DIST_DIR/skindemo-*.skin" "主题皮肤包(dist)"
    rm_old_glob "$ASSETS_SKIN_DIR/*.skin" "主题皮肤包(assets)"

    "$GRADLEW" "$SKIN_PKG_MODULE:assembleRelease"

    local apk="$SKIN_PKG_APK"
    [ -f "$apk" ] || { log_err "未生成皮肤 APK：$apk"; exit 1; }

    ensure_dist
    local out="$DIST_DIR/skindemo-${STAMP}.skin"
    cp -f "$apk" "$out"

    # 回填宿主内嵌皮肤（固定名，宿主启动按此名加载；皮肤是宿主资源，非主题库）
    mkdir -p "$ASSETS_SKIN_DIR"
    cp -f "$apk" "$ASSETS_SKIN_DIR/$ASSETS_SKIN_NAME"

    log_ok "主题皮肤包 → $out"
    log_ok "已回填宿主 assets → $ASSETS_SKIN_DIR/$ASSETS_SKIN_NAME"
}

# ---------- 打包比亚迪控件换肤桥（AAR） ----------
build_byd() {
    log_info "打包比亚迪控件换肤桥（AAR）…"
    rm_old_glob "$DIST_DIR/bydwidget-release-*.aar" "比亚迪换肤桥"

    "$GRADLEW" "$BYD_MODULE:assembleRelease"

    local aar="$BYD_AAR"
    [ -f "$aar" ] || { log_err "未生成 AAR：$aar"; exit 1; }

    ensure_dist
    local out="$DIST_DIR/bydwidget-release-${STAMP}.aar"
    cp -f "$aar" "$out"
    log_ok "比亚迪换肤桥 → $out"
}

# ---------- 清理 ----------
clean() {
    log_info "清理本次打包产物：$DIST_DIR/"
    rm -rf "$DIST_DIR"
    log_ok "清理完成"
}

usage() {
    sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'
}

main() {
    need_gradlew
    local target="${1:-all}"

    case "$target" in
        sdk)  build_sdk ;;
        skin) build_skin ;;
        byd)  build_byd ;;
        all)
            build_sdk
            build_skin
            ;;
        clean) clean ;;
        -h|--help|help) usage ;;
        *)
            log_err "未知参数：$target"
            usage
            exit 1
            ;;
    esac

    if [ "$target" != "clean" ] && [ "$target" != "help" ] && [ "$target" != "-h" ] && [ "$target" != "--help" ]; then
        printf '\n%s\n' "${C_GREEN}=== 打包完成，产物位于 $DIST_DIR/ ===${C_RESET}"
        ls -lh "$DIST_DIR" 2>/dev/null || true
    fi
}

main "$@"
