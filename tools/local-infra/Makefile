# hashmatrix 本地测试基础设施（macOS / kind + 1ms 镜像缓存）
# 详见 README.md。所有目标幂等。
SHELL := /usr/bin/env bash
HERE  := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

.PHONY: help infra up down warm status nuke clean
.DEFAULT_GOAL := help

help: ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-10s\033[0m %s\n",$$1,$$2}'

infra: ## 仅拉起 5 个镜像缓存（不建集群）
	@"$(HERE)registries.sh" up

up: ## 拉起缓存 + kind 集群（幂等；常用）
	@"$(HERE)up.sh"

warm: ## 按 images.txt 预热镜像到缓存（可选，加速首次部署）
	@"$(HERE)warm.sh"

status: ## 查看缓存与集群状态
	@"$(HERE)registries.sh" status
	@echo "kind clusters:"; kind get clusters 2>/dev/null | sed 's/^/  /' || true

down: ## 删除 kind 集群，保留缓存（镜像不丢）
	@"$(HERE)down.sh"

nuke: ## 删除集群 + 清空缓存卷（彻底重置，会重新下载）
	@"$(HERE)down.sh" || true
	@"$(HERE)registries.sh" nuke

clean: down ## down 的别名
