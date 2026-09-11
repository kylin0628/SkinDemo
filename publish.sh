#!/usr/bin/env bash
#
# SkinDemo 一键发布脚本：把换肤 SDK（skinlibrary）发布到 GitHub Packages
#
# 用法：
#   ./publish.sh              # 发布稳定版（版本取 SKIN_VERSION 或默认 1.0.0）
#   ./publish.sh snapshot     # 发布快照版 1.0.0-SNAPSHOT（可覆盖，供联调迭代）
#   ./publish.sh 2.1.0        # 发布指定版本 2.1.0
#   ./publish.sh local        # 仅发布到本地 ~/.m2，供本机验证（无需凭证/网络）
#
# 凭证（二选一，勿提交到 git）：
#   1) 环境变量：export GPR_USER=<GitHub 用户名> GPR_KEY=<PAT>
#   2) 用户级 ~/.gradle/gradle.properties：
#        gpr.user=<GitHub 用户名>
#        gpr.key=<PAT 需 write:packages 权限>
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

GRADLEW="$ROOT/gradlew"

if [ -t 1 ]; then
    C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'; C_RESET=$'\033[0m'
else
    C_GREEN=""; C_YELLOW=""; C_RED=""; C_RESET=""
fi
log_info() { printf '%s\n' "${C_YELLOW}[INFO]${C_RESET} $*"; }
log_ok()   { printf '%s\n' "${C_GREEN}[ OK ]${C_RESET} $*"; }
log_err()  { printf '%s\n' "${C_RED}[FAIL]${C_RESET} $*" >&2; }

# 判断凭证是否可用：环境变量 → 用户级 gradle.properties → 项目级（仅兜底，不推荐）
has_cred() {
    [ -n "${GPR_USER:-}" ] && [ -n "${GPR_KEY:-}" ] && return 0
    local f="$HOME/.gradle/gradle.properties"
    if [ -f "$f" ] && grep -qE '^\s*gpr\.user\s*=' "$f" && grep -qE '^\s*gpr\.key\s*=' "$f"; then
        return 0
    fi
    return 1
}

usage() {
    sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
}

main() {
    [ -x "$GRADLEW" ] || [ -f "$GRADLEW" ] || { log_err "找不到 gradlew：$GRADLEW"; exit 1; }

    local target="${1:-release}"
    local version=""

    case "$target" in
        snapshot)
            version="1.0.0-SNAPSHOT"
            ;;
        local)
            log_info "发布到本地 ~/.m2（验证用）…"
            "$GRADLEW" ":skinlibrary:publishReleasePublicationToMavenLocal" -PskinVersion=1.0.0-LOCAL
            log_ok "已发布到 ~/.m2/repository/com/kylin/skinlibrary/"
            return 0
            ;;
        -h|--help|help)
            usage
            return 0
            ;;
        release)
            version="${SKIN_VERSION:-1.0.0}"
            ;;
        *)
            # 用户直接传版本号，如 ./publish.sh 2.1.0
            version="$target"
            ;;
    esac

    # 远程发布前校验凭证
    if ! has_cred; then
        log_err "缺少 GitHub Packages 凭证。请配置："
        log_err "  环境变量 GPR_USER / GPR_KEY，或"
        log_err "  ~/.gradle/gradle.properties 中的 gpr.user / gpr.key（PAT 需 write:packages 权限）"
        exit 1
    fi

    log_info "发布 skinlibrary $version → GitHub Packages …"
    # 主题库仅发布换肤能力，不内置皮肤包（皮肤是宿主资源）
    "$GRADLEW" ":skinlibrary:publishReleasePublicationToGitHubPackagesRepository" -PskinVersion="$version"

    log_ok "发布完成：com.kylin:skinlibrary:$version"
    log_ok "下游依赖坐标：implementation 'com.kylin:skinlibrary:$version'"
}

main "$@"
