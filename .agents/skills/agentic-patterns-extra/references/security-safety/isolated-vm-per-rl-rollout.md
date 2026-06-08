---
title: "Isolated VM Per RL Rollout"
status: "proposed"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["WebAssembly sandboxing", "RL safety isolation"]
category: "Security & Safety"
source: "https://github.com/nibzard/awesome-agentic-patterns"
tags: [vm-isolation, rl-rollout, sandbox, security, runtime-isolation, wasm]
---

## Problem

When deploying new reinforcement learning policies or model updates in production, there is significant risk of
unexpected behavior. A misconfigured or poorly trained policy can execute arbitrary code, make unauthorized API calls,
or corrupt data. Running new policies in the same environment as stable ones creates blast radius that can affect the
entire system.

## Solution

Run each RL policy rollout in an isolated virtual machine or sandbox environment. The VM provides a clean execution
boundary with its own filesystem, network, and resource limits. The policy can only interact with the outside world
through well-defined, audited interfaces.

**Core components:**

- **VM isolation**: Each policy runs in a separate lightweight VM (e.g., WebAssembly, Firecracker microVM) with its own
  memory space and resource limits.
- **Controlled interfaces**: Policies can only communicate through predefined channels (e.g., a message queue, a
  specific API endpoint).
- **Resource caps**: CPU, memory, and network usage are bounded to prevent resource exhaustion.
- **Audit logging**: All actions taken by the policy are logged for post-deployment analysis.
- **Kill switch**: The parent system can terminate the VM immediately if the policy exhibits dangerous behavior.

**Implementation sketch:**

```python
class VMSandbox:
    def __init__(self, policy_id, resource_limits):
        self.policy_id = policy_id
        self.limits = resource_limits
        self.vm = create_microvm(resource_limits)
        self.log = AuditLogger()

    def run(self, policy_code, input_data):
        # Deploy policy code into isolated VM
        self.vm.deploy(policy_code)

        # Execute with bounded resources
        result = self.vm.execute(input_data, timeout=self.limits['timeout'])

        # Log all actions
        self.log.record(self.policy_id, result.actions)

        return result

    def kill(self):
        self.vm.terminate()
```

## How to use it

- Deploy new RL policies in isolated VMs before allowing them to interact with production systems.
- Gradually expand permissions as the policy proves safe through controlled testing.
- Use the kill switch immediately if the policy exhibits unexpected behavior.
- Review audit logs after each rollout to identify issues before expanding scope.

**When to apply:**

- Deploying new RL policies in production
- Running untrusted or experimental models
- Multi-tenant environments where one policy's failure should not affect others
- Compliance requirements that mandate isolation of autonomous systems

## Trade-offs

- **Pros:**
    - Complete isolation prevents policy failures from affecting the host system.
    - Resource limits prevent denial-of-service scenarios.
    - Audit logging enables post-incident analysis.
    - Kill switch provides immediate response to dangerous behavior.

- **Cons:**
    - VM overhead adds latency to policy execution.
    - Managing multiple VMs increases operational complexity.
    - Policies cannot directly access host resources (may require workarounds for legitimate needs).
    - VM creation and teardown adds time to deployment pipelines.

## References

- WebAssembly (WASM) sandboxing: https://wasm.org
- Firecracker microVMs: https://firecracker-microvm.github.io
- RL safety research: https://arxiv.org/abs/2301.13456
